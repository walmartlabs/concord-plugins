provider "aws" {
  profile    = "default"
  region     = "us-west-2"
}

resource "aws_instance" "example" {
  ami                    = "${data.aws_ami.ubuntu.id}"
  instance_type          = "t2.micro"
  vpc_security_group_ids = ["${aws_default_security_group.default.id}"]
  tags = {
    Name = "concord-test"
  }
}

data "aws_vpc" "default" {
  default = true
}

resource "aws_default_security_group" "default" {
  vpc_id = "${data.aws_vpc.default.id}"

  ingress {
    protocol  = -1
    self      = true
    from_port = 0
    to_port   = 0
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "image-type"
    values = ["machine"]
  }

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-bionic-18.04-amd64-server-*"]
  }
}

output "message" {
  value = "hello!"
}

variable "name" {}
variable "time" {}

output "name_from_varfile0" {
  value = "${var.name}"
}

output "time_from_varfile1" {
  value = "${var.time}"
}
