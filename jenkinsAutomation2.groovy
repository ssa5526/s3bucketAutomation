
pipeline {
    agent any

    //create global environment variables for use in pipeline
    environment{
        TF_VAR_region = "${aws_region}"
        TF_VAR_bucketName = "${bucket_name}"
        TF_VAR_accountName = "${Account}"
        TF_VAR_sysName = "${system_name}"
        TF_VAR_sysOwner = "${system_owner}"
        TF_VAR_bu = "${business_unit}"
        TF_VAR_cc = "${cost_center}"
        TF_VAR_dpmt = "${department}"
        TF_VAR_org = "${organization}"
        TF_VAR_tsm = "${technical_service_manager}"
        TF_VAR_kmsEnabled = "${kms_enabled}"
        TF_VAR_versioningEnabled = "${versioning_enabled}"
        TF_VAR_objectLockEnabled = "${object_lock_enabled}"
    }

    //parameters pulled from snow and transferred to environment variables
    parameters{
        string(name: 'aws_region', defaultValue: '', description: 'region where bucket is deployed',)
        string(name: 'bucket_name', defaultValue: '', description: 'name of bucket',)
        string(name: 'system_name', defaultValue: '', description: 'system name',)
        string(name: 'system_owner', defaultValue: '', description: 'SysOwner',)
        string(name: 'business_unit', defaultValue: '', description: 'BusinessUnit',)
        string(name: 'cost_center', defaultValue: '', description: 'CostCenter',)
        string(name: 'department', defaultValue: '', description: 'department',)
        string(name: 'organization', defaultValue: '', description: 'organization')
        string(name: 'technical_service_manager', defaultValue: '', description: 'technical service manager')
        choice(name: 'kms_enabled', choices: ['True', 'False'], description: 'Is KMS the security option, if not default to AES256',)
        choice(name: 'versioning_enabled', choices: ['True', 'False'], description: 'is versioning enabled on the bucket',)  
        choice(name: 'object_lock_enabled', choices: ['True', 'False'], description: 'is object lock enabled VERSIONING REQUIRED',)
        choice(name: 'LifecyclePolicy', choices: ['Tier 1', 'Tier 2', 'Tier 3'], description: 'lifecycle tier')
        choice(name: 'Account', choices: ['IT-CORP-A', 'IT-CORP-A', 'SHARED-SVCS', 'SHARED-SVCS-NONPROD'], description: 'Account',)
    }

    stages{
        stage('call tf bucket creation'){
            steps{
                script{
                    //based on account chooses the account number
                    if (params.Account == "Placeholder") {
                        env.accountNum = '111111111'
                    } 
                    if (params.Account == "Placeholder") {
                        env.accountNum = '111111111'
                    } 
                    if (params.Account == "Placeholder") {
                        env.accountNum = '111111111'
                    } 
                    if (params.Account == "Placeholder") {
                        env.accountNum = '111111111'
                    }

                    //based on the answer to the choice, set one of the lifecycle policies active not sure if this is actually changing the env variable
                    if (params.LifecyclePolicy == 'Tier 1'){
                        env.TF_VAR_lifecycleTierOneEnabled == 'True'
                        env.TF_VAR_lifecycleTierTwoEnabled == 'False'
                        env.TF_VAR_lifecycleTierThreeEnabled == 'False'
                    }
                    if (params.LifecyclePolicy == 'Tier 2'){
                        env.TF_VAR_lifecycleTierOneEnabled == 'False'
                        env.TF_VAR_lifecycleTierTwoEnabled == 'True'
                        env.TF_VAR_lifecycleTierThreeEnabled == 'False'
                    }
                    if (params.lifecyclePolicy == 'Tier 3'){
                        env.TF_VAR_lifecycleTierOneEnabled == 'False'
                        env.TF_VAR_lifecycleTierTwoEnabled == 'False'
                        env.TF_VAR_lifecycleTierThreeEnabled == 'True'
                    }

                    //adds aws utility and creates a json file for the bucket data can use this for importing via bucketInfo.auto.tfvars.json not being used currently
                    def awsUtil = new AwsUtil()
                    awsUtil.withAccountRoleArn('arn:aws:iam::$accountNum:role/Cloudbees_Jenkins', 'us-east-1' , {
                        //exports the variables to the tf file 
                        try {
                            sh '''
                                terraform init -no-color
                                export TF_VAR_region="${aws_region}"
                                export TF_VAR_bucketName="${bucket_name}"
                                export TF_VAR_accountName="${Account}"
                                export TF_VAR_sysName="${system_name}"
                                export TF_VAR_sysOwner="${system_owner}"
                                export TF_VAR_bu="${business_unit}"
                                export TF_VAR_cc="${cost_center}"
                                export TF_VAR_dpmt="${department}"
                                export TF_VAR_org="${organization}"
                                export TF_VAR_tsm="${technical_service_manager}"
                                export TF_VAR_kmsEnabled="${kms_enabled}"
                                export TF_VAR_versioningEnabled="${versioning_enabled}"
                                export TF_VAR_objectLockEnabled="${object_lock_enabled}"
                                export TF_VAR_lifecycleTierOneEnabled="${lifecycleTierOneEnabled}"
                                export TF_VAR_lifecycleTierTwoEnabled="${lifecycleTierTwoEnabled}"
                                export TF_VAR_lifecycleTierThreeEnabled="${lifecycleTierThreeEnabled}"

                                terraform init -no-color
                                terraform validate -no-color
                                terraform plan -no-color
                                terraform apply -auto-approve 
                                terraform output 
                            '''
                        }catch(err){
                            echo "Caught: ${err}"
                            sh '''
                                terraform destroy -auto-approve
                                echo "Terraform destroyed"	
                            '''
                            echo "Failure"
                        }
                    }
                )}
            }
        }
    }
}
