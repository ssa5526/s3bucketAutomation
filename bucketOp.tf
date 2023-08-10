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
    CreatedBy    = "Terraform"
  }

  #object lock is enabled
  #object_lock_enabled = "${var.objectLockEnabled}" == "true" ? true : false
}

/*
#bucket policy assignment
resource "aws_s3_bucket_policy" "s3policy" {
  bucket = aws_s3_bucket.s3bucket.id
  policy = data.aws_iam_policy_document.s3policy.json
}

data "aws_iam_policy_document" "s3policy" {
  statement {
    principals {
      type = "AWS"
      identifiers = ["*"]
    }

    actions = split(",", var.bucketPolicies)

    resources = [aws_s3_bucket.s3bucket.arn, "${aws_s3_bucket.s3bucket.arn}/*"]

    condition{
      test = "Bool"
      variable = "aws:SecureTransport"
      values = ["true"]
    }
  }

}
*/

#bucket serverside encryption policy, either kms or AES256
resource "aws_s3_bucket_server_side_encryption_configuration" "sse" {
  bucket = aws_s3_bucket.s3bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = var.kmsEnabled == "True" ? "aws:kms" : "AES256"
    }
  }
}

#Bucket access. All public access is blocked by default
resource "aws_s3_bucket_public_access_block" "public-access-block" {
  bucket = aws_s3_bucket.s3bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

#bucket versioning
resource "aws_s3_bucket_versioning" "versioning" {
  bucket = aws_s3_bucket.s3bucket.id

  versioning_configuration {
    status = (var.versioningEnabled == "True") ? "Enabled" : "Disabled"
  }
}

#lifecycle policies
resource "aws_s3_bucket_lifecycle_configuration" "lifecyclePolicy"{
  bucket = aws_s3_bucket.s3bucket.id

  #rule one
  rule{
    id = "Application Data"
    #blank filter means it applies to all data in the bucket
    filter {}
    
    transition {
      days          = 30
      storage_class = "INTELLIGENT_TIERING"
    }
    status = var.lifecycleTierOneEnabled == "True" ? "Enabled" : "Disabled"
  }

  #rule two
  rule{
    id = "Backup Data"
    filter {}

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
    status = var.lifecycleTierTwoEnabled == "True" ? "Enabled" : "Disabled"
  }

  #rule three
  rule{
    id = "Logging Data"
    filter {}  
    

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

    status = var.lifecycleTierThreeEnabled == "True" ? "Enabled" : "Disabled"
  }

}

#initiializing the backend. Backend configuration is done in jenkinsfile
terraform{
  backend "s3"{
  }
}


