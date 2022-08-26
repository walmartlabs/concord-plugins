variable "aws_access_key" {}
variable "aws_secret_key" {}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region     = "us-west-2"
  access_key = var.aws_access_key
  secret_key = var.aws_secret_key
}

data "aws_vpc" "default" {
  default = true
}

resource "aws_key_pair" "deployer" {
  key_name   = "concord-TF-ITs-key"
  public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDOaV1IVyqzYbUXc6RAgVbVxrbNGMKEsLtLXW82FOBUI4x6+bBJVKNqa/c5cntFOQgRm2bs6CfQPmLJFAOlA1gbRXiOrfatX4WYt6p0Z54BqMCaXZ09zqLcInUPmP8eSTmxqKrefJQAsLqJi46knKxdnPqFxAqbfwed2THu3KokUmWoDceZFEuLb/gXkf6uXinFSV1Gp27ulzNFZpxxDUG9357kmJro0nXAObaOCpCCfb3gG2T/TXGaKtCk7tLIS+iSEzYxLWkmTnVciB9qOIRNrw4TmhEUrrcOz8W0e/WB/BtS4Y56y3cCcibx8WPbWMYSRCmOZPLWbY/x13nA92R9pW+j5dmcWjqsYypnZaQMSQumIUl2/TLOCF0/Fphlj8/G0ZMxIjYXEk5g3ZorTvUR1+TJ7Cbrwk4NIROFbpHVm60w+svnqDgNv+cRDDSOmhB5SDOjBvqiVAqtlb8lE8km5M3VbRZ+12QBDR3ILwPRcKoqK6HnVa4jiODweoOSXos= test@concord"
}


output "message" {
  value = "hello!"
}

variable "name" {}
variable "time" {}

output "default_vpc_id" {
  value = data.aws_vpc.default.id
}

output "name_from_varfile0" {
  value = var.name
}

output "time_from_varfile1" {
  value = var.time
}
