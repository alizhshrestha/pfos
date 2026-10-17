#!/usr/bin/env python3
"""Seed demo transactions through PFOS REST API; requires only Python 3."""
import argparse
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

# category suffix, description, type suffix, payment suffix, amount
SAMPLES = [
    ('food', 'Milk & bread', 'debit', 'cash', 120),
    ('food', 'Weekly groceries', 'debit', 'esewa', 2450.50),
    ('food', 'Coffee with friends', 'debit', 'bank', 350),
    ('health', 'Pharmacy medicine', 'debit', 'cash', 850),
    ('health', 'Hospital consultation', 'debit', 'bank', 1500),
    ('health', 'Insurance reimbursement', 'credit', 'bank', 1200),
    ('household', 'Cleaning supplies', 'debit', 'esewa', 650),
    ('household', 'Kitchen repair', 'debit', 'cash', 2200),
    ('household', 'Household purchase refund', 'credit', 'esewa', 450),
    ('income', 'Monthly salary', 'credit', 'bank', 65000),
    ('income', 'Performance bonus', 'credit', 'connectips', 5000),
    ('income', 'Interest received', 'credit', 'bank', 185.75),
    ('debt', 'Loan repayment', 'debit', 'connectips', 5000),
    ('debt', 'Personal debt repayment', 'debit', 'cash', 2000),
    ('debt', 'Loan installment', 'debit', 'bank', 3500),
    ('shopping', 'Running shoes', 'debit', 'bank', 4200),
    ('shopping', 'Notebook purchase', 'debit', 'cash', 180),
    ('shopping', 'Shopping refund', 'credit', 'bank', 900),
    ('transportation', 'Bus fare', 'debit', 'cash', 35),
    ('transportation', 'Taxi ride', 'debit', 'esewa', 450),
    ('transportation', 'Petrol refill', 'debit', 'bank', 1800),
    ('utilities', 'Electricity bill', 'debit', 'esewa', 1350),
    ('utilities', 'Internet bill', 'debit', 'connectips', 1500),
    ('utilities', 'Water bill', 'debit', 'cash', 250),
    ('work', 'Freelance project payment', 'credit', 'connectips', 18000),
    ('work', 'Project hosting cost', 'debit', 'bank', 1200),
    ('work', 'Consulting payment', 'credit', 'bank', 7500),
    ('personal_care', 'Haircut', 'debit', 'cash', 300),
    ('personal_care', 'Shampoo purchase', 'debit', 'esewa', 480),
    ('personal_care', 'Skincare supplies', 'debit', 'bank', 950),
    ('entertainment', 'Movie tickets', 'debit', 'esewa', 700),
    ('entertainment', 'Quiz competition winnings', 'credit', 'cash', 1000),
    ('entertainment', 'Music subscription', 'debit', 'bank', 199),
    ('donation', 'Community donation', 'debit', 'connectips', 500),
    ('donation', 'Food drive contribution', 'debit', 'cash', 300),
    ('donation', 'School fundraiser', 'debit', 'esewa', 750),
]
DATES = ['2026-09-30', '2026-10-01', '2026-10-13', '2026-10-17', '2026-10-31', '2026-11-01']


def transactions():
    return [
        {
            'transactionDate': DATES[index % len(DATES)],
            'description': f'[DEMO] {index + 1:02d} {description}',
            'categoryId': f'cat_{category}',
            'transactionTypeId': f'type_{direction}',
            'paymentMethodId': f'method_{method}',
            'amount': amount,
            'notes': 'PFOS demo seed v1 — fictional test data.',
        }
        for index, (category, description, direction, method, amount) in enumerate(SAMPLES)
    ]


def request(base, path, payload=None):
    body = None if payload is None else json.dumps(payload).encode('utf-8')
    headers = {'Accept': 'application/json'}
    if body is not None:
        headers['Content-Type'] = 'application/json'
    req = Request(base + path, data=body, headers=headers,
                  method='GET' if payload is None else 'POST')
    try:
        with urlopen(req, timeout=15) as response:
            result = json.load(response)
    except HTTPError as error:
        raise RuntimeError(f'HTTP {error.code}: {error.read().decode()}') from error
    except (URLError, TimeoutError) as error:
        raise RuntimeError(f'Cannot reach API: {error}. Check Spring Boot and MongoDB.') from error
    if result.get('success') is not True:
        raise RuntimeError(f'API error: {result.get("error")}')
    return result['data']


def seed(base, rows):
    # Validate all required master values before creating any transaction.
    for endpoint, field in [('categories', 'categoryId'),
                            ('transaction-types', 'transactionTypeId'),
                            ('payment-methods', 'paymentMethodId')]:
        values = request(base, '/' + endpoint)
        allowed = {value['_id'] for value in values if value.get('isActive', True)}
        missing = {row[field] for row in rows} - allowed
        if missing:
            raise RuntimeError(f'Missing/inactive {endpoint}: {sorted(missing)}')

    existing = set()
    page = 0
    while True:
        query = urlencode({'search': '[DEMO]', 'page': page, 'size': 100})
        result = request(base, '/transactions?' + query)
        existing.update(row['description'] for row in result['items'])
        page += 1
        if page >= result['totalPages']:
            break

    created = skipped = 0
    for row in rows:
        if row['description'] in existing:
            skipped += 1
            print('SKIP  ', row['description'])
            continue
        # Do not retry POST automatically: a timeout can occur after a successful insert.
        print('CREATE', row['description'], flush=True)
        result = request(base, '/transactions', row)
        created += 1
        existing.add(row['description'])
        print('       ID:', result['_id'])
    print(f'Finished: {created} created, {skipped} skipped.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080/api',
                        help='API URL including /api (default: %(default)s)')
    parser.add_argument('--dry-run', action='store_true',
                        help='Print the 36 request bodies without making network requests')
    args = parser.parse_args()
    rows = transactions()
    if args.dry_run:
        print(json.dumps(rows, indent=2))
        return 0
    try:
        seed(args.base_url.rstrip('/'), rows)
    except (RuntimeError, ValueError, KeyError, OSError) as error:
        print(f'Stopped: {error}', file=sys.stderr)
        print('Earlier successful inserts remain. Check history before rerunning after a timeout.',
              file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
