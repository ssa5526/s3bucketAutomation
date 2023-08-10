@Library('DevOpsLib')
import com.regeneron.radit.devops.aws.AwsUtil

pipeline {
    agent any

    //create global environment variables for export into terraform file. "TF_VAR_" is a required prefix so terraform can recognize them and associate
    //them with their correct variable in the variables.tf file
    environment{
        TF_VAR_region = "${aws_region}"
        TF_VAR_bucketName = "${bucket_name}"
        TF_VAR_sysName = "${system_name}"
        TF_VAR_sysOwner = "${system_owner}"
        TF_VAR_bu = "${business_unit}"
        TF_VAR_cc = "${cost_center}"
        TF_VAR_dpmt = "${department}"
        TF_VAR_org = "${organization}"
        TF_VAR_tsm = "${technical_service_manager}"
        TF_VAR_kmsEnabled = "${kms_enabled}"
        TF_VAR_versioningEnabled = "${versioning_enabled}"
    }

    //parameters pulled from snow and transferred to environment variables
    parameters{
        //region where bucket is deployed, what account, and what is the buckets name.
        string(name: 'aws_region', defaultValue: "us-east-1", description: 'region where bucket is deployed',)
        string(name: 'bucket_name', defaultValue: "regn-awsops-automation-test-", description: 'name of bucket',)
        choice(name: 'Account', choices: ['PHOENIX-DTE', 'IT-CORP-A', 'IT-CORP-A', 'SHARED-SVCS', 'SHARED-SVCS-NONPROD'], description: 'USE PHOENIX-DTE',)

        //tags input
        string(name: 'system_name', defaultValue: 'sysname', description: 'system name',)
        string(name: 'system_owner', defaultValue: 'sysown', description: 'System Owner',)
        string(name: 'business_unit', defaultValue: 'bu', description: 'Business Unit',)
        string(name: 'cost_center', defaultValue: 'cc', description: 'Cost Center',)
        string(name: 'department', defaultValue: 'dpt', description: 'Department',)
        string(name: 'organization', defaultValue: 'org', description: 'Organization')
        string(name: 'technical_service_manager', defaultValue: 'tsm', description: 'Technical Service Manager')

        //is this bucket being created or destroyed
        choice(name: 'execution_status', choices: ['create', 'destroy'], description: 'Create or Destroy a bucket. Make sure the name of the bucket is spelled correctly and the location of the tfstate file is correct')

        //what bucket is the tfstate stored in? 
        string(name: 'tf_state_bucket', defaultValue: 'statetfteraform', description: 'Where is the tfstate stored')
        string(name: 'tf_state_region', defaultValue: 'us-east-1', description: 'What region is the tfstate bucket in')

        //choices for whether kms is enabled, versioning enabled, or what of the 3 default lifecycle policies.
        choice(name: 'kms_enabled', choices: ['True', 'False'], description: 'Is KMS the security option, if not default to AES256?')
        choice(name: 'versioning_enabled', choices: ['True', 'False'], description: 'Is versioning enabled on the bucket?')  
        choice(name: 'LifecyclePolicy', choices: ['Application Data', 'Backup Data', 'Logging Data'], description: 'Which lifecycle tier based on https://confluence.regeneron.com/display/ITCLOUDDEVOPS/CLD+-+S3+Tiering+Strategy?')
        
    }

    stages{
        stage('call tf bucket creation'){
            steps{
                script{
                    //adds aws utility for creating the bucket via iam role and region
                    def awsUtil = new AwsUtil()

                    //based on account chooses the account number. Account needs role called cloudbees jenkins with s3:*
                    if (params.Account == "IT-CORP-A") {
                        env.accountNum = '694928589017'
                    } 
                    if (params.Account == "IT-CORP-B") {
                        env.accountNum = '486047195917'
                    } 
                    if (params.Account == "SHARED-SVCS") {
                        env.accountNum = '265776368239'
                    } 
                    if (params.Account == "SHARED-SVCS-NONPROD") {
                        env.accountNum = '703834445914'
                    }
                    if (params.Account == "PHOENIX-DTE") {
                        env.accountNum = '956649439491'
                    }
                    
                    //defaults all lifecycle tiers to false for initialization
                    def tierOneEnabled = 'False'
                    def tierTwoEnabled = 'False'
                    def tierThreeEnabled = 'False'

                    //based on the answer to the choice, set one of the lifecycle policies active not sure if this is actually changing the env variable
                    if (params.LifecyclePolicy == 'Application Data'){
                        tierOneEnabled = 'True'
                        tierTwoEnabled = 'False'
                        tierThreeEnabled = 'False'
                    }
                    if (params.LifecyclePolicy == 'Backup Data'){
                        tierOneEnabled = 'False'
                        tierTwoEnabled = 'True'
                        tierThreeEnabled = 'False'
                    }
                    if (params.LifecyclePolicy == 'Logging Data'){
                        tierOneEnabled = 'False'
                        tierTwoEnabled = 'False'
                        tierThreeEnabled = 'True'
                    }                   

                    //change based on account role that is wished to be used
                    awsUtil.withAccountRoleArn("arn:aws:iam::$accountNum:role/Cloudbees_Jenkins", "$params.aws_region" , {
                        //if the execution status is set to destroy, we initialize the bucket from its tfstate and then we destroy it based off of that, the tfstate remains as a archived file
                        if (params.execution_status == 'destroy'){
                            sh """
                                terraform init -no-color -backend-config="key=${bucket_name}.tfstate" -backend-config="region=${tf_state_region}" -backend-config="bucket=${tf_state_bucket}"
                                terraform init
                                echo "Destroy existing bucket"
                                terraform destroy -auto-approve
                            """
                            currentBuild.result = 'SUCCESS'
                            return
                        }else{
                            //exports the variables to the tf file. This also finds the bucket to store the tfstate file.
                            //this is stored in a try and catch to make sure that any error immediately destroys the bucket before any greater problems occur
                            //the first set of terraform init and exports allow all configuration details to be initialized. The next 5 commands execute the bucketCreation.tf file
                            try {
                                sh """
                                    terraform init -no-color -backend-config="key=${bucket_name}.tfstate" -backend-config="region=${tf_state_region}" -backend-config="bucket=${tf_state_bucket}"
                                    export TF_VAR_region="${aws_region}"
                                    export TF_VAR_bucketName="${bucket_name}"
                                    export TF_VAR_sysName="${system_name}"
                                    export TF_VAR_sysOwner="${system_owner}"
                                    export TF_VAR_bu="${business_unit}"
                                    export TF_VAR_cc="${cost_center}"
                                    export TF_VAR_dpmt="${department}"
                                    export TF_VAR_org="${organization}"
                                    export TF_VAR_tsm="${technical_service_manager}"
                                    export TF_VAR_kmsEnabled="${kms_enabled}"
                                    export TF_VAR_versioningEnabled="${versioning_enabled}"
                                    export TF_VAR_lifecycleTierOneEnabled="${tierOneEnabled}"
                                    export TF_VAR_lifecycleTierTwoEnabled="${tierTwoEnabled}"
                                    export TF_VAR_lifecycleTierThreeEnabled="${tierThreeEnabled}"
                                    terraform init -no-color 

                                    terraform validate -no-color
                                    terraform plan -no-color
                                    terraform apply -auto-approve
                                """
                            }catch(err){
                                echo "Caught: ${err}"
                                sh """
                                    terraform destroy -auto-approve
                                    echo "Terraform destroyed"	
                                """
                                echo "Failure"
                            }

                        }                    
                    })
                }
            }
        }
    }
}