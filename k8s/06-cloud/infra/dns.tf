resource "yandex_dns_zone" "clinic" {
  name   = "dental-zone"
  zone   = "${var.domain}."
  public = true
}

resource "yandex_iam_service_account" "dns" {
  name = "dental-dns-sa"
}

resource "yandex_resourcemanager_folder_iam_member" "dns" {
  folder_id = var.folder_id
  role      = "dns.editor"
  member    = "serviceAccount:${yandex_iam_service_account.dns.id}"
}
