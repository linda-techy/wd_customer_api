-- Add FK constraint for project_id if not exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints
                   WHERE constraint_name = 'fk_support_ticket_project') THEN
        ALTER TABLE support_tickets
        ADD CONSTRAINT fk_support_ticket_project
        FOREIGN KEY (project_id) REFERENCES customer_projects(id);
    END IF;
END $$;
