terraform {
  required_version = ">= 1.10.0, < 2.0.0"

  required_providers {
    yandex = {
      source  = "registry.terraform.io/yandex-cloud/yandex"
      version = "~> 0.244.0"
    }
  }
}

provider "yandex" {
  cloud_id  = var.cloud_id
  folder_id = var.folder_id
  zone      = var.zone
}
