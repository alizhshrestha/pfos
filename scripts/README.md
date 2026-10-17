# Demo transactions

Requires Python 3, running Spring Boot on port 8080, and MongoDB. Uses the existing REST API so normal transaction validation still applies. No Python packages are required.

From the `pfos` project root, preview without making any requests:

```sh
python3 backend/scripts/seed_demo_transactions.py --dry-run
```

Insert the data:

```sh
python3 backend/scripts/seed_demo_transactions.py
```

From the `backend` directory, use `python3 scripts/seed_demo_transactions.py` instead.

For another local API port:

```sh
python3 backend/scripts/seed_demo_transactions.py --base-url http://localhost:8081/api
```

The script checks master IDs, reads existing demo descriptions across all pages, then POSTs missing entries sequentially. It stops on the first failure and prints each created ID. Successful earlier inserts remain. Run one copy at a time. Matching descriptions are skipped on reruns; renamed or soft-deleted demo records can be inserted again. This is a simple seed convenience, not server-enforced idempotency. After a timeout, inspect history before rerunning because the server may still complete the request.

## Dataset and checks

36 fictional records, all prefixed `[DEMO]`, across 12 categories, both transaction types, all four payment methods, and six fixed dates. Transfer is intentionally excluded because V1 does not track internal money movement. Dates are fixed so test expectations stay repeatable; they are not relative to today.

Combine these filters with search `[DEMO]` to exclude real entries:

| Filter | Expected demo results |
| --- | ---: |
| Search `[DEMO]` only | 36 |
| From `2026-10-01`, to `2026-10-31` | 24 |
| From and to `2026-10-17` | 6 |
| Category Food & Groceries | 3 |
| Category Health + type Credit | 1 |
| Payment method Cash | 10 |
| From and to `2026-12-01` | 0 |

Search `milk & bread` to check literal special characters; search `MILK` to check case-insensitive matching. With `[DEMO]` and 10 rows per page, expect four pages (10, 10, 10, 6). Counts assume the initial unmodified seed data. Refunds and reimbursements use positive amounts with Credit.

The script does not update or delete existing records, and does not run automatically with Spring Boot.
