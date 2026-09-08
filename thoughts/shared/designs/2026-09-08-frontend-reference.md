---
date: 2026-09-08
topic: "Frontend Reference — Page/API/Endpoint Map"
status: draft
---

# Nexus OMS Frontend Reference

A map of the React frontend: which pages exist, which API modules they call, and the backend endpoints behind them. Built from `frontend/src/pages`, `frontend/src/api`, and the shared type contract in `frontend/src/types/index.ts`.

## How to read this

- **Pages** live in `frontend/src/pages/*.tsx` (~96 files).
- **API modules** live in `frontend/src/api/*.ts` (~63 files) and `frontend/src/api/connectors/*` (marketplace connectors).
- Every API call goes through the shared `client` (axios) wrapper in `frontend/src/api/client.ts`, which returns the `ApiResponse<T>` envelope.
- **Data contracts** are the exported TypeScript interfaces in `frontend/src/types/index.ts` (1645 lines). These mirror the backend DTOs.

## Shared API envelope

All responses use `ApiResponse<T>`:

```ts
ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  errors?: string[]
  pagination?: { page, limit, total, totalPages }
}
```

List endpoints support `PaginationParams` (`page`, `limit`, `sortBy`, `sortOrder`) plus domain filters.

---

## Domain 1 — Auth & Tenant

**Pages:** `LoginPage`, `LaunchPadPage`, `NotFoundPage`

**API module:** `auth.ts`

| Endpoint | Method | Purpose |
|---|---|---|
| `/auth/login` | POST | Login (username/password), returns `AuthResponse` |
| `/auth/mfa/verify` | POST | TOTP verification |
| `/auth/sso/${provider}` | POST | SSO login |
| `/auth/forgot-password` | POST | Request reset |
| `/auth/reset-password` | POST | Set new password |
| `/auth/register` | POST | Register (provisions tenant) |
| `/auth/tenants` | GET | List available tenants |
| `/auth/sso/providers` | GET | List SSO providers |
| `/auth/refresh` | POST | Refresh access token |

**Key types:** `AuthResponse`, `LoginRequest`, `RegisterRequest`, `MfaVerificationRequest`, `SsoLoginRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `TenantInfo`, `User`

**Note:** `AuthResponse` carries `tenantId`, `tenantName`, `role`, `permissions`, `securityGroups`. Tenant provisioning on registration now populates `tenantName`.

---

## Domain 2 — Orders & Fulfillment

**Pages:** `OrdersPage`, `OrderDetailPage`, `CreateOrderPage`, `FindOrderPage`, `FulfillmentPage`, `TaskQueuesPage`, `B2BPortalPage`, `BOPISPage`, `StoreDashboardPage`, `PreOrdersPage`

**API modules:** `orders.ts`, `newBackend.ts` (shared), `aiOrders.ts`

**orders.ts endpoints:**

| Endpoint | Method | Purpose |
|---|---|---|
| `/orders` | GET | List (filters) |
| `/orders/${id}` | GET | Detail |
| `/orders` | POST | Create |
| `/orders/${id}` | PUT | Update status |
| `/orders/${id}/confirm` | POST | Confirm |
| `/orders/${id}/allocate` | POST | Allocate inventory |
| `/orders/${id}/ship` | POST | Ship (carrierId, trackingNumber) |
| `/orders/${id}/cancel` | POST | Cancel |
| `/orders/${id}/split` | POST | Split order |
| `/orders/merge` | POST | Merge orders |

**newBackend.ts order endpoints:** `/orders`, `/orders/${id}`, `/orders/stats`

**Key types:** `Order`, `OrderItem`, `OrderStatus`, `OrderChannel`, `OrderFilters`, `OrderTimelineEvent`, `FulfillmentType`, `Priority`, `OrderAllocation`, `AllocationResult`

---

## Domain 3 — Inventory & Products

**Pages:** `InventoryPage`, `InventoryEnhancedPage`, `ProductsPage`, `InventoryReceivingPage`, `CycleCountPage`, `ReplenishmentPage`, `SlottingOptimizationPage`, `ATPRulesPage`

**API modules:** `inventory.ts`, `products.ts`, `inventoryReceipts.ts`, `cycleCounts.ts`, `atp.ts`, `replenishment.ts`, `slotting.ts`, `newBackend.ts`

**inventory.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/inventory` | GET | List |
| `/inventory/${sku}` | GET | By SKU |
| `/inventory/adjust` | POST | Adjust qty (sku, quantity, reason) |

**products.ts:** `/products` GET/POST, `/products/${id}` GET/PUT/DELETE

**inventoryReceipts.ts:** `/inventory-receipts` GET/POST, `/inventory-receipts/${id}` GET, `/inventory-receipts/${id}/receive` POST

**cycleCounts.ts:** `/cycle-counts` GET/POST, `/cycle-counts/${id}` GET, `/cycle-counts/${id}/count` POST

**atp.ts:** `/atp/rules/${id}` DELETE, `/atp/allocate` POST, `/atp/release` POST

**Key types:** `Inventory`, `Product`, `InventoryReceipt`, `CycleCount`, `InventoryFilters`

---

## Domain 4 — Shipping, Carriers & Rate Shopping

**Pages:** `ShippingPage`, `CarriersPage`, `CarrierRateShoppingPage`, `LabelPrintingPage`, `ManifestPage`, `FreightAuditPage`, `AiLoadingPage`

**API modules:** `shipping.ts`, `carriers.ts`, `rateShopping.ts`, `freightAudit.ts`, `newBackend.ts`

**shipping.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/shipments` | GET/POST | List/create shipments |
| `/shipments/${id}` | GET | Detail |
| `/shipments/${id}/ship` | POST | Ship |
| `/shipments/${id}/deliver` | POST | Mark delivered |
| `/shipments/${id}/void` | POST | Void |
| `/shipping/kpis` | GET | KPIs |

**carriers.ts:** `/carriers` GET/POST, `/carriers/${id}` GET/PUT/DELETE, `/carriers/kpis` GET

**rateShopping.ts:** `/rate-shopping/shop` POST, `/rate-shopping/best` POST

**newBackend.ts:** `/labels` GET, `/labels/generate` POST, `/manifests` GET/POST, `/manifests/${id}` PATCH

**Key types:** `Shipment`, `ShipmentLabel`, `ShipmentEvent`, `ShipmentStatus`, `RateQuote`, `RateShoppingResult`, `CarrierPerformance`

---

## Domain 5 — Warehouse Operations

**Pages:** `WarehousePage`, `WarehouseDashboardPage`, `PickingPage`, `PackerScreen`, `PackingPage`, `PickersPage`, `WavePlanningPage`, `YardDockPage`, `LaborManagementPage`

**API modules:** `warehouse.ts`, `picking.ts`, `packing.ts`, `pickers.ts`, `wavePlanning.ts`, `yardManagement.ts`, `laborManagement.ts`, `newBackend.ts`

**warehouse.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/warehouses` | GET/POST | List/create |
| `/warehouses/${id}` | GET/PUT/DELETE | Detail/update/delete |
| `/warehouses/${id}/zones` | GET | Zones |
| `/warehouses/${id}/bins` | GET | Bins |
| `/warehouses/${id}/bins/empty` | GET | Empty bins |
| `/warehouses/${id}/equipment` | GET | Equipment |
| `/warehouses/${id}/staff` | GET | Staff |
| `/warehouses/${id}/summary` | GET | Summary |
| `/warehouses/zones` | POST | Create zone |
| `/warehouses/bins` | POST | Create bin |
| `/warehouses/equipment` | POST | Create equipment |
| `/warehouses/staff` | POST | Create staff |
| `/warehouses/bins/${id}/reserve` | PUT | Reserve bin |
| `/warehouses/bins/${id}/release` | PUT | Release bin |
| `/warehouses/equipment/${id}/status` | PUT | Update status |
| `/warehouses/staff/${id}/increment-picks` | PUT | Increment picks |

**picking.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/picking/lists` | GET/POST | Picklists |
| `/picking/lists/${id}` | GET | Detail |
| `/picking/lists/${id}/items` | GET | Items |
| `/picking/lists/${id}/assign` | POST | Assign picker |
| `/picking/lists/${id}/pick-all` | POST | Pick all |
| `/picking/lists/${id}/complete` | POST | Complete |
| `/picking/lists/${id}/cancel` | POST | Cancel |
| `/picking/items/${itemId}/pick` | POST | Pick item |
| `/orders/stats` | GET | Stats |

**packing.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/packing/packages` | GET/POST | Packages |
| `/packing/packages/${id}` | GET | Detail |
| `/packing/packages/${id}/start` | POST | Start |
| `/packing/packages/${id}/items` | POST | Add item |
| `/packing/packages/${id}/complete` | POST | Complete |
| `/packing/packages/${id}/label` | POST | Generate label |
| `/packing/packages/${id}/ship` | POST | Ship |
| `/packing/packages/${id}/void` | POST | Void |

**Key types:** `Warehouse`, `WarehouseZone`, `WarehouseBin`, `WarehouseEquipment`, `WarehouseStaff`, `Picklist`, `PicklistItem`, `NxPackage`

---

## Domain 6 — Order Routing, Allocation & Exceptions

**Pages:** `OrderRoutingPage`, `BrokeringQueuePage`, `RoutingRulesPage`, `TaskQueuesPage`, `AiOrderRoutingPage`

**API modules:** `orderRouting.ts`, `brokering.ts`, `routingRules.ts`, `aiAgents.ts`

**orderRouting.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/order-routing/allocations` | GET | Allocations |
| `/order-routing/allocations/${orderId}` | GET | By order |
| `/order-routing/exceptions` | GET | Exceptions |
| `/order-routing/exceptions/${id}` | GET | Exception detail |
| `/order-routing/kpis` | GET | KPIs |
| `/order-routing/allocate` | POST | Allocate |
| `/order-routing/reallocate` | POST | Reallocate |
| `/order-routing/simulate` | POST | Simulate |

**routingRules.ts:** `/routing-rules` GET/POST, `/routing-rules/${id}` GET/PUT/DELETE, `/routing-rules/reorder` PUT

**brokering.ts:** `/brokering/queue/${id}` DELETE

**Key types:** `OrderAllocation`, `FulfillmentException`, `RoutingRule`, `AllocationResult`

---

## Domain 7 — Returns & Post-Sale

**Pages:** `ReturnsPage`, `ReturnsEnhancedPage`, `ReturnsFinancePage`, `PaymentsPage`

**API modules:** `returns.ts`, `returnsFinance.ts`, `newBackend.ts`

**returns.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/returns` | GET/POST | List/create |
| `/returns/${id}` | GET | Detail |
| `/returns/kpis` | GET | KPIs |
| `/returns/reasons` | GET | Reasons |
| `/returns/${id}/approve` | POST | Approve |
| `/returns/${id}/receive` | POST | Receive |
| `/returns/${id}/inspect` | POST | Inspect |
| `/returns/${id}/refund` | POST | Refund |
| `/returns/${id}/reject` | POST | Reject |
| `/returns/${id}/status` | PUT | Update status |

**returnsFinance.ts:** `/returns-finance/impact`, `/returns-finance/returns/${returnId}/credit-memos`, `/returns-finance/credit-memos/${id}/deposit`, `/returns-finance/credit-memos/split`, `/returns-finance/exchange/lesser-value`

**Key types:** `Return`, `ReturnItem`, `ReturnStatus`, `Disposition`, `RefundStatus`

---

## Domain 8 — Procurement & Suppliers

**Pages:** `ProcurementPage`

**API module:** `procurement.ts`

| Endpoint | Method | Purpose |
|---|---|---|
| `/procurement/suppliers` | GET/POST | Suppliers |
| `/procurement/suppliers/${id}` | GET/PUT/DELETE | Supplier detail |
| `/procurement/suppliers/${id}/contacts` | GET | Contacts |
| `/procurement/suppliers/${id}/contracts` | GET | Contracts |
| `/procurement/supplier-contacts` | POST | Add contact |
| `/procurement/supplier-contracts` | POST | Add contract |
| `/procurement/requests` | GET/POST | Purchase requests |
| `/procurement/requests/${id}` | GET | Request detail |
| `/procurement/requests/${id}/submit` | POST | Submit |
| `/procurement/requests/${id}/approve` | POST | Approve |
| `/procurement/requests/${requestId}/items` | POST | Add item |
| `/procurement/requests/${id}/status` | PUT | Update status |
| `/procurement/purchase-orders` | GET/POST | Purchase orders |
| `/procurement/purchase-orders/${id}` | GET | PO detail |
| `/procurement/purchase-orders/${id}/approve` | POST | Approve |
| `/procurement/purchase-orders/${poId}/receive` | POST | Receive |
| `/procurement/purchase-orders/${id}/status` | PUT | Update status |
| `/procurement/rfqs` | GET/POST | RFQs |
| `/procurement/rfqs/${id}` | GET | RFQ detail |
| `/procurement/rfqs/${id}/submit` | POST | Submit |
| `/procurement/rfqs/${rfqId}/responses` | GET/POST | Responses |

**Key types:** `Supplier`, `SupplierContact`, `SupplierContract`, `PurchaseRequest`, `PurchaseOrder`, `Rfq`, `RfqResponse`

---

## Domain 9 — Finance, Invoicing & Billing

**Pages:** `InvoicingPage`, `PaymentsPage`, `BillingStatementsPage`, `RateCardsPage`, `ClientPortalPage`, `B2BPortalPage`

**API modules:** `invoicing.ts`, `billing.ts`

**invoicing.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/invoicing/invoices` | GET/POST | Invoices |
| `/invoicing/invoices/${id}` | GET | Detail |
| `/invoicing/invoices/${invoiceId}/payments` | GET/POST | Payments |
| `/invoicing/invoices/${id}/status` | PUT | Update status |
| `/invoicing/payments` | GET | Payments |
| `/invoicing/payments/${id}` | GET | Payment detail |
| `/invoicing/payments/${paymentId}/refund` | POST | Refund |
| `/invoicing/credit-memos` | GET/POST | Credit memos |
| `/invoicing/credit-memos/${id}` | GET | Detail |

**billing.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/billing/statements` | GET | Statements |
| `/billing/statements/${id}` | GET | Detail |
| `/billing/statements/generate` | POST | Generate |
| `/billing/statements/${id}/status` | PATCH | Update status |
| `/rate-cards` | GET/POST | Rate cards |
| `/rate-cards/${id}` | GET/PUT/DELETE | Rate card detail |
| `/client-portal/overview` | GET | Client overview |

**Key types:** `Invoice`, `InvoiceItem`, `Payment`, `CreditMemo`, `CreditMemoItem`

---

## Domain 10 — Integrations & Marketplaces

**Pages:** `IntegrationHubPage`, `IntegrationMarketplacePage`, `IntegrationStoresPage`, `ShopifyIntegrationPage`, `BigCommercePage`, `AmazonIntegrationPage`, `EbayIntegrationPage`, `WalmartIntegrationPage`, `SyncConflictsPage`, `ImportExportCenter`, `EdiAutomationPage`, `EmailOrderParsingPage`, `AutomationSystemsPage`, `EndlessAislePage`

**API modules:** `integrationHub.ts`, `integrationStores.ts`, `bigcommerce.ts`, `connectors/*`, `sync.ts`, `importApi.ts`, `integrationPlatform.ts`, `edi.ts`, `emailParser.ts`, `automation.ts`, `endlessAisle.ts`

**integrationHub.ts:** `/integration-hub/connectors`, `/integration-hub/connectors/${id}`, `/integration-hub/connectors/${id}/sync/${syncType}`, `/integration-hub/connectors/${id}/test`, `/integration-hub/connectors/${id}/webhooks`, `/integration-hub/jobs`, `/integration-hub/jobs/${jobId}`, `/integration-hub/platforms`

**integrationStores.ts:** `/integration-stores` GET/POST, `/integration-stores/${id}` GET/PUT/DELETE, `/integration-stores/${id}/settings`, `/integration-stores/${id}/sync-logs`, `/integration-stores/${id}/sync-status`, plus `/shopify/stores/${storeId}/sync/{fulfillments|inventory|orders|products|refunds}` and `/shopify/stores/${storeId}/webhooks/register`

**bigcommerce.ts:** `/integrations/bigcommerce/config` GET/PUT, `/integrations/bigcommerce/sync/{orders|products|inventory|shipments|refunds}`, `/integrations/bigcommerce/webhooks/register`, `/integrations/bigcommerce/sync-logs`

**Marketplace connectors** (`api/connectors/*`): Amazon, eBay, Walmart — hit `/api/v1` endpoints; `connectorRegistry` provides `fetchAllStatus` for marketplace health.

**sync.ts:** `/sync/conflicts`, `/sync/conflicts/${id}`, `/sync/conflicts/${id}/resolve`, `/sync/field-mappings` GET/POST, `/sync/reconciliation`, `/sync/recovery`

**importApi.ts:** `/import/entity-types`, `/import/formats`, `/import/modes`, `/import/history`, `/import/history/${id}`, `/import/history/${id}/logs`, `/import/history/${id}/reprocess`, `/import/history/${id}/download/original`, `/import/history/${id}/download/errors`, `/import/${entityType}` POST, `/import/token`, `/sample-data/entity-types`, `/sample-data/generate/${entityType}`

**edi.ts:** `/edi`, `/edi/${id}`, `/edi/parse`, `/edi/upload`, `/edi/partners` GET/POST, `/edi/${id}/reprocess`, `/edi/kpis`

**emailParser.ts:** `/email-parser`, `/email-parser/${id}`, `/email-parser/parse`, `/email-parser/parse-csv`, `/email-parser/${id}/approve`, `/email-parser/${id}/reject`, `/email-parser/kpis`

**Key types:** `IntegrationStore`, `IntegrationStoreSetting`, `SyncTypeStatus`, `StoreSyncStatus`, `ConnectorMetadata`, `ConnectorInstance`, `BatchJob`, `SyncLog`, `SyncResult`, `ImportResult`, `ImportHistorySummary`, `EdiDocument`, `EdiPartner`, `EmailParsedOrder`

---

## Domain 11 — AI / ML Platform

**Pages:** `AiPage`, `AiPlatformPage`, `AiBriefingPage`, `AiForecastingPage`, `AiLoadingPage`, `AiPackingPage`, `AiOrderRoutingPage`, `AiExperimentsPage`, `AiAuditTrailPage`

**API modules:** `ai.ts`, `aiPlatform.ts`, `aiAgents.ts`, `experimentApi.ts`, `chatApi.ts`, `aiOrders.ts`

**ai.ts:** `/ai/models` GET, `/ai/predict/{carrier|demand|inventory}` POST

**aiPlatform.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/ai/models` | GET/POST | Model registry |
| `/ai/models/${id}` | GET/PUT | Model detail/update |
| `/ai/models/summary` | GET | Summary |
| `/ai/models/${modelId}/versions` | GET/POST | Versions |
| `/ai/models/${modelId}/deploy/${versionId}` | POST | Deploy |
| `/ai/models/${modelId}/rollback/${versionId}` | POST | Rollback |
| `/ai/predict/${modelType}` | POST | Predict |
| `/ai/predict/direct/${modelId}/${versionId}` | POST | Direct predict |
| `/ai/training/jobs` | GET/POST | Training jobs |
| `/ai/training/jobs/${jobId}` | GET | Job detail |
| `/ai/training/jobs/${jobId}/start` | POST | Start |
| `/ai/training/jobs/${jobId}/complete` | POST | Complete |
| `/ai/training/jobs/${jobId}/fail` | POST | Fail |
| `/ai/features` | GET/POST | Feature defs |
| `/ai/features/groups` | GET | Feature groups |
| `/ai/fallbacks/${modelId}` | GET | Rule fallbacks |
| `/ai/monitoring/dashboard` | GET | Monitoring |
| `/ai/monitoring/models/${modelId}` | GET | Model monitoring |
| `/ai/analytics/dashboard` | GET | Analytics |
| `/ai/models/${modelId}/inference-logs` | GET | Inference logs |

**aiAgents.ts:** `/ai/briefing`, `/ai/forecasting`, `/ai/loading`, `/ai/packing`, `/ai/routing`, `/ai/routing/queue`, `/ai/recommendations/${id}/respond`

**experimentApi.ts:** `/ai/experiments` GET/POST, `/ai/experiments/${id}` GET/PUT, `/ai/experiments/${id}/{start|complete|fail|rollback}` POST

**chatApi.ts:** `/ai/chat` POST

**Key types:** `AiModel`, `AiPlatformModel`, `TrainingRun`, `AiModelVersion`, `AiTrainingJob`, `AiFeatureDefinition`, `AiDeployment`, `AiInferenceLog`, `AiRuleFallback`, `AiExperiment`, `AiSuggestion`, `AiActionHistory`, `AiExecuteRequest`

---

## Domain 12 — RBAC, Users & Settings

**Pages:** `UsersPage`, `SettingsPage`, `OrderApprovalsPage`, `RejectionsPage`

**API modules:** `rbac.ts`, `settings.ts`, `approvals.ts`, `rejections.ts`

**rbac.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/rbac/permissions` | GET/POST | Permissions |
| `/rbac/permissions/role` | GET | Role permissions |
| `/rbac/check-permission` | GET | Check permission |
| `/rbac/teams` | GET/POST | Teams |
| `/rbac/user-roles` | GET/POST | User-role assignments |
| `/rbac/user-roles/${id}` | DELETE | Remove assignment |

**settings.ts:** `/settings` GET/PUT

**approvals.ts:** `/approvals/rules/${id}` DELETE

**rejections.ts:** `/rejections/reasons/${id}` DELETE

**Key types:** `UserRole`, `RolePermission`, `UserRoleAssignment`, `Team`, `CompanySettings`, `User`

---

## Domain 13 — Notifications, Documents & Audit

**Pages:** `NotificationsCenter`, `DocumentsPage`, `AuditPage`

**API modules:** `notifications.ts`, `documents.ts`, `audit.ts`

**notifications.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/notifications/templates` | GET/POST | Templates |
| `/notifications/templates/${id}` | GET/PUT | Template detail |
| `/notifications/logs` | GET | Logs |
| `/notifications/alerts` | GET/POST | Alert rules |
| `/notifications/alerts/${id}/toggle` | PUT | Toggle |
| `/notifications/unread-count` | GET | Unread count |
| `/notifications/send` | POST | Send |

**documents.ts:** `/documents` GET/POST, `/documents/${id}` GET/PUT/DELETE, `/documents/${documentId}/versions` GET/POST, `/documents/by-entity` GET

**audit.ts:** `/integration-platform/audit`, `/integration-platform/audit/entity`

**Key types:** `NotificationTemplate`, `NotificationLog`, `AlertRule`, `Document`, `DocumentVersion`, `AuditEntry`, `AuditPage`

---

## Domain 14 — Warehouse RF / Scan / RFID

**Pages:** `RfidPage`, `PickingPage`, `PackerScreen`

**API modules:** `rfid.ts`, `scan.ts`, `pickup.ts`

**rfid.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/rfid/decode/${epc}` | GET | Decode EPC |
| `/rfid/inventory` | GET | RFID inventory |
| `/rfid/sessions` | POST | Create session |
| `/rfid/sessions/${sessionId}/epcs` | POST | Add EPCs |
| `/rfid/sessions/${sessionId}/receive` | POST | Receive |
| `/rfid/sessions/${sessionId}/cycle-count` | POST | Cycle count |
| `/rfid/sessions/${sessionId}/complete` | POST | Complete |
| `/rfid/retag` | POST | Retag |

**scan.ts:** `/rf/search` GET

**pickup.ts:** (BOPIS) — used by `BopisAppPage`, `BopisOwnerPage`, `StoreDashboardPage`

---

## Domain 15 — Analytics & Reporting

**Pages:** `AnalyticsPage`, `AnalyticsDashboardPage`, `ReportBuilderPage`, `DashboardPage`, `WarehouseDashboardPage`, `LoaderScreen`, `YardDockPage`

**API modules:** `analytics.ts`, `newBackend.ts`

**analytics.ts:**
| Endpoint | Method | Purpose |
|---|---|---|
| `/analytics/dashboard` | GET | Dashboard |
| `/analytics/activity` | GET | Activity feed |
| `/analytics/alerts` | GET | Alerts |
| `/analytics/carrier-performance` | GET | Carrier perf |
| `/analytics/cost-breakdown` | GET | Cost breakdown |
| `/analytics/lanes` | GET | Shipping lanes |
| `/analytics/order-status-distribution` | GET | Status dist |
| `/analytics/orders/velocity` | GET | Order velocity |
| `/analytics/returns` | GET | Returns analytics |
| `/analytics/task-queue-summary` | GET | Task queue |
| `/warehouses/summary` | GET | Warehouse summary |

**newBackend.ts:** `/dashboard`, `/reports`, `/reports/dashboard`, `/reports/scheduled`, `/reports/generate`, `/reports/scheduled` POST, `/task-queues` GET, `/task-queues/${id}` PATCH

**Key types:** `DashboardKPI`, `OrderVelocity`, `CarrierPerformance`

---

## Domain 16 — MCP Server

**Pages:** `McpServerPage`

**API module:** `mcp.ts`

| Endpoint | Method | Purpose |
|---|---|---|
| `/mcp/tools` | GET/POST | Tool registry |
| `/mcp/execute` | POST | Execute tool |
| `/mcp/agents/${agentName}/usage` | GET | Agent usage |
| `/mcp/agents/budgets` | GET/POST | Agent budgets |

---

## Cross-cutting: `newBackend.ts`

A consolidated API module used by many "enhanced" pages (`InventoryEnhancedPage`, `CreateOrderPage`, `LabelPrintingPage`, `ManifestPage`, `PaymentsPage`, `ReportBuilderPage`, `WavePlanningPage`, `ReturnsEnhancedPage`). Centralizes `/dashboard`, `/orders`, `/inventory`, `/products`, `/returns`, `/waves`, `/picking/lists`, `/packing/queues`, `/labels`, `/manifests`, `/reports`, `/task-queues`, `/settings`, `/warehouses`, `/invoicing` endpoints.

---

## Open Questions

- **Pages with no API import** (`AiAuditTrailPage`, `AiPage` variants, `LaunchPadPage`, `OfflinePage`, `WarehouseDashboardPage`, `NotFoundPage`) may use direct `client` calls or be static/shell pages — worth a follow-up pass to confirm.
- **Connector endpoints** (`connectors/*`) build URLs dynamically against `/api/v1`; the exact routes aren't enumerated here.
- Some API modules (`automation.ts`, `endlessAisle.ts`, `laborManagement.ts`, `promotions.ts`, `replenishment.ts`, `slotting.ts`, `transfers.ts`, `freightAudit.ts`, `fulfillmentLimits.ts`, `parkedOrders.ts`, `pickers.ts`, `pickup.ts`, `wavePlanning.ts`, `yardManagement.ts`) returned empty endpoint lists — likely dynamic URL building or direct fetch. Worth verifying if those pages need full coverage.
