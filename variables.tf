variable "region" {
  description = "region where bucket is deployed"
  type        = string
  default     = "us-east-1"
}

variable "bucketName" {
  description = "name of the bucket"
  type        = string

  # this does not work
  #validation {
  #  condition     = can(regex("^regn(-[a-z]+)+$", var.bucketName))
  #  error_message = "bucket name does not follow bucket naming policy, must be in format regn-xxx-xxx"
  #}
}

variable "sysName" {
  description = "System Name"
  type        = string
}

variable "sysOwner" {
  description = "System Owner"
  type        = string
}

variable "bu" {
  description = "Business Unit"
  type        = string
}

variable "cc" {
  description = "Cost Center"
  type        = string
}

variable "dpmt" {
  description = "Department"
  type        = string
}

variable "org" {
  description = "Organization"
  type        = string
}

variable "tsm" {
  description = "Technical Service Manager"
  type        = string
}

variable "kmsEnabled" {
  description = "is SSE-KMS enabled, if not use AES256"
  type        = string
  default     = "False"
}

variable "versioningEnabled" {
  description = "Is versioning enabled on bucket"
  type        = string
  default     = "False"
}




#configuration for lifecycle rules being active or not is handled inside of jenkinsfile

variable "lifecycleTierOneEnabled" {
  description = "default lifecycle rules are enabled"
  type        = string
  default     = "False"
}

variable "lifecycleTierTwoEnabled" {
  description = "default lifecycle rules are enabled"
  type        = string
  default     = "False"
}

variable "lifecycleTierThreeEnabled" {
  description = "default lifecycle rules are enabled"
  type        = string
  default     = "False"
}
