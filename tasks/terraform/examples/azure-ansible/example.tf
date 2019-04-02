variable "client_id" {}
variable "client_secret" {}
variable "subscription_id" {}
variable "tenant_id" {}
variable "ssh_public_key" {}

provider "azurerm" {
  client_id       = "${var.client_id}"
  client_secret   = "${var.client_secret}"
  subscription_id = "${var.subscription_id}"
  tenant_id       = "${var.tenant_id}"
}

resource "azurerm_resource_group" "demo" {
  name     = "demo-team"
  location = "southcentralus"
}

resource "azurerm_virtual_network" "demonet" {
  name = "demonet"

  address_space = [
    "10.0.0.0/16",
  ]

  location            = "southcentralus"
  resource_group_name = "${azurerm_resource_group.demo.name}"

  tags {
    environment = "Terraform Demo"
  }
}

resource "azurerm_subnet" "demosubnet" {
  name                 = "demosubnet"
  resource_group_name  = "${azurerm_resource_group.demo.name}"
  virtual_network_name = "${azurerm_virtual_network.demonet.name}"
  address_prefix       = "10.0.2.0/24"
}

resource "azurerm_public_ip" "demopublicip" {
  name                = "demopublicip"
  location            = "southcentralus"
  resource_group_name = "${azurerm_resource_group.demo.name}"
  allocation_method   = "Dynamic"

  tags {
    environment = "Terraform Demo"
  }
}

resource "azurerm_network_security_group" "demosg" {
  name                = "demosg"
  location            = "southcentralus"
  resource_group_name = "${azurerm_resource_group.demo.name}"

  security_rule {
    name                       = "SSH"
    priority                   = 1001
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  tags {
    environment = "Terraform Demo"
  }
}

resource "azurerm_network_interface" "demonic" {
  name                      = "demonic"
  location                  = "southcentralus"
  resource_group_name       = "${azurerm_resource_group.demo.name}"
  network_security_group_id = "${azurerm_network_security_group.demosg.id}"

  ip_configuration {
    name                          = "demoniccfg"
    subnet_id                     = "${azurerm_subnet.demosubnet.id}"
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = "${azurerm_public_ip.demopublicip.id}"
  }

  tags {
    environment = "Terraform Demo"
  }
}

resource "random_id" "randomId" {
  keepers = {
    resource_group = "${azurerm_resource_group.demo.name}"
  }

  byte_length = 8
}

resource "azurerm_storage_account" "demostorageacc" {
  name                     = "demod${random_id.randomId.hex}"
  resource_group_name      = "${azurerm_resource_group.demo.name}"
  location                 = "southcentralus"
  account_replication_type = "LRS"
  account_tier             = "Standard"

  tags {
    environment = "Terraform Demo"
  }
}

resource "azurerm_virtual_machine" "demovm" {
  name                = "demovm"
  location            = "southcentralus"
  resource_group_name = "${azurerm_resource_group.demo.name}"

  network_interface_ids = [
    "${azurerm_network_interface.demonic.id}",
  ]

  vm_size = "Standard_DS1_v2"

  storage_os_disk {
    name              = "demoosdisk"
    caching           = "ReadWrite"
    create_option     = "FromImage"
    managed_disk_type = "Premium_LRS"
  }

  storage_image_reference {
    publisher = "Canonical"
    offer     = "UbuntuServer"
    sku       = "16.04.0-LTS"
    version   = "latest"
  }

  os_profile {
    computer_name  = "demo"
    admin_username = "azureuser"
  }

  os_profile_linux_config {
    disable_password_authentication = true

    ssh_keys {
      path     = "/home/azureuser/.ssh/authorized_keys"
      key_data = "${var.ssh_public_key}"
    }
  }

  boot_diagnostics {
    enabled     = "true"
    storage_uri = "${azurerm_storage_account.demostorageacc.primary_blob_endpoint}"
  }

  tags {
    environment = "Terraform Demo"
  }
}

data "azurerm_public_ip" "dataip" {
  name                = "${azurerm_public_ip.demopublicip.name}"
  resource_group_name = "${azurerm_virtual_machine.demovm.resource_group_name}"
}

output "public_ip" {
  value = "${data.azurerm_public_ip.dataip.ip_address}"
}
