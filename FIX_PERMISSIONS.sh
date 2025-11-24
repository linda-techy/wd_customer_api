#!/bin/bash

# Fix File Permissions for Storage Folder
# This script fixes the permission issue causing 500 errors

echo "🔧 Fixing storage folder permissions..."

# 1. Check current user running the backend
echo "1. Checking backend service user..."
sudo systemctl show -p User wd-cust-api

# 2. Fix ownership (change to backend user or make readable by all)
echo "2. Making files readable by backend service..."

# Option A: Change ownership to backend user (recommended)
# sudo chown -R wd-cust-api:wd-cust-api /home/ftpuser/var/www/app/walldotbuilders/storage/

# Option B: Keep current owner but make files readable by group/others
sudo chmod -R 755 /home/ftpuser/var/www/app/walldotbuilders/storage/
sudo chmod -R 644 /home/ftpuser/var/www/app/walldotbuilders/storage/projects/1/documents/*.pdf

# 3. Add backend user to walldotgroup (if needed)
echo "3. Adding backend user to walldotgroup..."
BACKEND_USER=$(sudo systemctl show -p User wd-cust-api | cut -d= -f2)
if [ "$BACKEND_USER" != "root" ] && [ "$BACKEND_USER" != "" ]; then
    sudo usermod -a -G walldotgroup $BACKEND_USER
    echo "Added $BACKEND_USER to walldotgroup"
fi

# 4. Verify permissions
echo "4. Verifying permissions..."
ls -la /home/ftpuser/var/www/app/walldotbuilders/storage/projects/1/documents/

echo "✅ Permissions fixed!"
echo ""
echo "Now restart the backend:"
echo "sudo systemctl restart wd-cust-api"

