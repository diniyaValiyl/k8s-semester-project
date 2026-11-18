variable "cloud_id" {
  type = string
}

variable "folder_id" {
  type = string
}

variable "domain" {
  type = string

  validation {
    condition     = can(regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$", var.domain))
    error_message = "Use a domain without a scheme, path or trailing dot."
  }
}

variable "admin_cidr" {
  type = string

  validation {
    condition     = can(cidrnetmask(var.admin_cidr)) && var.admin_cidr != "0.0.0.0/0"
    error_message = "Use your public IPv4 address with /32, or a trusted network CIDR."
  }
}

variable "zone" {
  type    = string
  default = "ru-central1-a"
}
