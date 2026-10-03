# Approval workflow contract v1

Only a department head scoped to the transcript's department can approve or reject it. Approval accepts a PDF containing two server-verified signatures and stores it as a new immutable revision linked by `supersedesTranscriptId`; the prior lecturer revision is preserved. The verified revision records the `DEPT_APPROVED` and `ARCHIVED` audit transitions. Rejection requires a reason, retains the existing PDF, and sends an in-app notice to the lecturer.
