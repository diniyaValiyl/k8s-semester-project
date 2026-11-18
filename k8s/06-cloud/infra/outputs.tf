output "cluster_id" {
  value = yandex_kubernetes_cluster.clinic.id
}

output "folder_id" {
  value = var.folder_id
}

output "domain" {
  value = var.domain
}

output "registry_id" {
  value = yandex_container_registry.clinic.id
}

output "dns_zone_id" {
  value = yandex_dns_zone.clinic.id
}

output "dns_service_account_id" {
  value = yandex_iam_service_account.dns.id
}

output "gwin_service_account_id" {
  value = yandex_iam_service_account.gwin.id
}

output "alb_security_group_id" {
  value = yandex_vpc_security_group.alb.id
}

output "subnet_id" {
  value = yandex_vpc_subnet.clinic.id
}
