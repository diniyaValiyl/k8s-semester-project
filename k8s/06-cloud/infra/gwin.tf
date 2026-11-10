resource "yandex_iam_service_account" "gwin" {
  name = "dental-gwin-sa"
}

resource "yandex_resourcemanager_folder_iam_member" "gwin" {
  for_each = toset([
    "alb.editor",
    "vpc.publicAdmin",
    "certificate-manager.certificates.downloader",
    "certificate-manager.editor",
    "compute.viewer",
    "k8s.viewer",
  ])

  folder_id = var.folder_id
  role      = each.value
  member    = "serviceAccount:${yandex_iam_service_account.gwin.id}"
}

resource "yandex_vpc_security_group" "alb" {
  name       = "dental-alb-sg"
  network_id = yandex_vpc_network.clinic.id

  ingress {
    protocol       = "TCP"
    port           = 80
    v4_cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    protocol       = "TCP"
    port           = 443
    v4_cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    protocol          = "TCP"
    port              = 30080
    predefined_target = "loadbalancer_healthchecks"
  }

  egress {
    protocol       = "TCP"
    from_port      = 0
    to_port        = 65535
    v4_cidr_blocks = yandex_vpc_subnet.clinic.v4_cidr_blocks
  }
}
