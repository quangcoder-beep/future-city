-- Clear old orders that might have fallback coordinates
DELETE FROM Orders;
-- Optionally reset identity
DBCC CHECKIDENT ('Orders', RESEED, 0);
GO
PRINT 'Orders table cleared for fresh GPS testing.';
