variable "client_id" {}
variable "client_secret" {}
variable "subscription_id" {}
variable "tenant_id" {}
variable "name" {}
variable "time" {}

terraform {
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = "3.17.0"
    }
  }
}

provider "azurerm" {
  client_id       = var.client_id
  client_secret   = var.client_secret
  subscription_id = var.subscription_id
  tenant_id       = var.tenant_id

  features {}
}

resource "azurerm_resource_group" "mygroup" {
  name     = "concord-tf-test-resources"
  location = "southcentralus"

  lifecycle {
    ignore_changes = [ tags ]
  }
}


output "resource_group_info" {
  value = {
    name      = azurerm_resource_group.mygroup.name,
    id        = azurerm_resource_group.mygroup.id,
    location  = azurerm_resource_group.mygroup.location
  }
}

output "name_from_varfile0" {
  value = var.name
}

output "time_from_varfile1" {
  value = var.time
}
