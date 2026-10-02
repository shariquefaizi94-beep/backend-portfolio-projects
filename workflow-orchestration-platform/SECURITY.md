# Security Policy

## Reporting Vulnerabilities

If you discover a security vulnerability, please report it responsibly by emailing the repository owner rather than opening a public issue.

## Security Practices

- Dependencies are scanned with OWASP Dependency Check in CI
- No secrets or credentials are committed to the repository
- All database credentials use environment variables
- Input validation on all API endpoints
- Parameterized queries via JPA (no SQL injection risk)
- Rate limiting should be configured in production deployments
