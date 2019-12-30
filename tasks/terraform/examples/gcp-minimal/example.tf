variable "credentials" {}
variable "projectName" {}

provider "google" {
  credentials = "${var.credentials}"
  project = "${var.projectName}"
  region = "us-central1"
  zone = "us-central1-a"
}

resource "google_compute_network" "vpc_network" {
  name = "my-vpc-network"
}

resource "google_compute_instance" "vm_instance" {
  name = "my-vm-instance"

  machine_type = "f1-micro"
  zone = "us-central1-a"

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-9"
    }
  }

  network_interface {
    subnetwork = "${google_compute_network.vpc_network.name}"
  }
}
