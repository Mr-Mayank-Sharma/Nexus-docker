-- Tenant provisioning: add plan to company settings
-- New tenants are provisioned with plan='trial' on registration (see AuthService.provisionTenant)
ALTER TABLE nx_company_settings ADD COLUMN IF NOT EXISTS plan VARCHAR(50) NOT NULL DEFAULT 'trial';