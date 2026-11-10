resource "yandex_vpc_network" "clinic" {
  name = "dental-network"
}

resource "yandex_vpc_subnet" "clinic" {
  name           = "dental-subnet"
  zone           = var.zone
  network_id     = yandex_vpc_network.clinic.id
  v4_cidr_blocks = ["10.10.0.0/24"]
}

resource "yandex_vpc_security_group" "cluster" {
  name       = "dental-cluster-sg"
  network_id = yandex_vpc_network.clinic.id

  ingress {
    protocol          = "ANY"
    predefined_target = "self_security_group"
  }

  ingress {
    protocol          = "TCP"
    predefined_target = "loadbalancer_healthchecks"
    from_port         = 0
    to_port           = 65535
  }

  ingress {
    protocol       = "TCP"
    port           = 443
    v4_cidr_blocks = [var.admin_cidr]
  }

  ingress {
    protocol       = "TCP"
    port           = 6443
    v4_cidr_blocks = [var.admin_cidr]
  }

  ingress {
    protocol       = "ANY"
    v4_cidr_blocks = ["10.112.0.0/16", "10.96.0.0/16"]
  }

  ingress {
    protocol       = "TCP"
    from_port      = 30000
    to_port        = 32767
    v4_cidr_blocks = yandex_vpc_subnet.clinic.v4_cidr_blocks
  }

  ingress {
    protocol       = "TCP"
    port           = 10501
    v4_cidr_blocks = yandex_vpc_subnet.clinic.v4_cidr_blocks
  }

  egress {
    protocol       = "ANY"
    v4_cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "yandex_iam_service_account" "cluster" {
  name = "dental-cluster-sa"
}

resource "yandex_iam_service_account" "nodes" {
  name = "dental-nodes-sa"
}

resource "yandex_resourcemanager_folder_iam_member" "cluster" {
  for_each = toset(["k8s.clusters.agent", "vpc.publicAdmin"])

  folder_id = var.folder_id
  role      = each.value
  member    = "serviceAccount:${yandex_iam_service_account.cluster.id}"
}

resource "yandex_resourcemanager_folder_iam_member" "nodes" {
  folder_id = var.folder_id
  role      = "container-registry.images.puller"
  member    = "serviceAccount:${yandex_iam_service_account.nodes.id}"
}

resource "yandex_kubernetes_cluster" "clinic" {
  name                    = "dental-cluster"
  network_id              = yandex_vpc_network.clinic.id
  cluster_ipv4_ranges     = ["10.112.0.0/16"]
  service_ipv4_range      = "10.96.0.0/16"
  service_account_id      = yandex_iam_service_account.cluster.id
  node_service_account_id = yandex_iam_service_account.nodes.id
  release_channel         = "REGULAR"
  network_policy_provider = "CALICO"

  master {
    zonal {
      zone      = var.zone
      subnet_id = yandex_vpc_subnet.clinic.id
    }

    public_ip          = true
    security_group_ids = [yandex_vpc_security_group.cluster.id]
  }

  depends_on = [
    yandex_resourcemanager_folder_iam_member.cluster,
    yandex_resourcemanager_folder_iam_member.nodes,
  ]
}

resource "yandex_kubernetes_node_group" "clinic" {
  name       = "dental-nodes"
  cluster_id = yandex_kubernetes_cluster.clinic.id

  instance_template {
    platform_id = "standard-v3"

    resources {
      cores         = 2
      core_fraction = 20
      memory        = 4
    }

    boot_disk {
      type = "network-hdd"
      size = 64
    }

    network_interface {
      subnet_ids         = [yandex_vpc_subnet.clinic.id]
      security_group_ids = [yandex_vpc_security_group.cluster.id]
      nat                = true
    }

    scheduling_policy {
      preemptible = true
    }
  }

  scale_policy {
    fixed_scale {
      size = 1
    }
  }

  deploy_policy {
    max_expansion   = 0
    max_unavailable = 1
  }

  allocation_policy {
    location {
      zone = var.zone
    }
  }
}

resource "yandex_container_registry" "clinic" {
  name = "dental-registry"
}
