provider "aws" {
  region = var.region
}

#creating the bucket resource
resource "aws_s3_bucket" "s3bucket" {
  #assign the bucket a name
  bucket = var.bucketName


  #set mandatory tags for bucket here
  tags = {
    SysName      = "${var.sysName}"
    SysOwner     = "${var.sysOwner}"
    BusinessUnit = "${var.bu}"
    CostCenter   = "${var.cc}"
    Department   = "${var.dpmt}"
    Organization = "${var.org}"
    TSM          = "${var.tsm}"
  }

  #object lock is enabled
  object_lock_enabled = "${var.objectLockEnabled}" == "true" ? true : false
}

#security kms or 256
resource "aws_s3_bucket_server_side_encryption_configuration" "sse" {
  bucket = aws_s3_bucket.s3bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = var.kmsEnabled == "true" ? "aws:kms" : "AES256"
    }
  }
}


#bucket versioning
resource "aws_s3_bucket_versioning" "versioning" {
  bucket = aws_s3_bucket.s3bucket.id

  versioning_configuration {
    status = (var.versioningEnabled == "true") ? "Enabled" : "Disabled"
  }
}

#object lock, requires versioning
resource "aws_s3_bucket_object_lock_configuration" "objectLock" {
  bucket = aws_s3_bucket.s3bucket.id

  rule {
    default_retention {
      mode = "COMPLIANCE"
      days = 5
    }
  }
}

#bucket acl
resource "aws_s3_bucket_public_access_block" "public-access-block" {
  bucket = aws_s3_bucket.s3bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

#creates the lifecycle rules
resource "aws_s3_bucket_lifecycle_configuration" "tierOneLifecycle" {
  bucket = aws_s3_bucket.s3bucket.arn

  rule {
    id = "archiveOne"

    filter {}

    status = var.lifecycleTierOneEnabled == "true" ? "Enabled" : "Disabled"


    transition {
      days          = 30
      storage_class = "INTELLIGENT_TIERING"
    }
  }

}

resource "aws_s3_bucket_lifecycle_configuration" "tierTwoLifecycle" {
  bucket = aws_s3_bucket.s3bucket.arn

  rule {
    id = "archiveTwo"

    filter {}

    status = var.lifecycleTierTwoEnabled == "true" ? "Enabled" : "Disabled"

    transition {
      days          = 0
      storage_class = "INTELLIGENT_TIERING"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = 730

    }
  }

}

resource "aws_s3_bucket_lifecycle_configuration" "tierThreeLifecycle" {
  bucket = aws_s3_bucket.s3bucket.arn

  rule {
    id = "archiveThree"

    filter {}

    status = var.lifecycleTierThreeEnabled == "true" ? "Enabled" : "Disabled"

    transition {
      days          = 30
      storage_class = "INTELLIGENT_TIERING"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = 365
    }
  }
}

//terraform {
//  backend "s3" {
//    bucket = "regn-cloudops-terraform-dev-tf"
//    key = "${bucketName}-${accountName}.tfstate"
//  }
//}

