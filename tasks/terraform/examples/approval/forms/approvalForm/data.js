data = {
    "submitUrl": "/api/service/custom_form/6cf435f0-b85c-42ea-a454-1ac308aff8c3/9fe117b3-5dec-4de2-afe7-1765a0e4e305/continue",
    "success": false,
    "definitions": {
        "plan": {
            "type": "string",
            "cardinality": "ONE_OR_NONE"
        },
        "approved": {
            "type": "boolean",
            "cardinality": "ONE_AND_ONLY_ONE"
        }
    },
    "values": {
        "processId": "6cf435f0-b85c-42ea-a454-1ac308aff8c3",
        "plan": "terraform: Refreshing Terraform state in-memory prior to plan...\n" +
            "terraform: The refreshed state will be used to calculate this plan, but will not be\n" +
            "terraform: persisted to local or remote state storage.\n" +
            "terraform: \n" +
            "terraform: \n" +
            "terraform: ------------------------------------------------------------------------\n" +
            "terraform: \n" +
            "terraform: An execution plan has been generated and is shown below.\n" +
            "terraform: Resource actions are indicated with the following symbols:\n" +
            "terraform:   + create\n" +
            "terraform: \n" +
            "terraform: Terraform will perform the following actions:\n" +
            "terraform: \n" +
            "terraform:   + aws_instance.example\n" +
            "terraform:       id:                                <computed>\n" +
            "terraform:       ami:                               \"ami-2757f631\"\n" +
            "terraform:       arn:                               <computed>\n" +
            "terraform:       associate_public_ip_address:       <computed>\n" +
            "terraform:       availability_zone:                 <computed>\n" +
            "terraform:       cpu_core_count:                    <computed>\n" +
            "terraform:       cpu_threads_per_core:              <computed>\n" +
            "terraform:       ebs_block_device.#:                <computed>\n" +
            "terraform:       ephemeral_block_device.#:          <computed>\n" +
            "terraform:       get_password_data:                 \"false\"\n" +
            "terraform:       host_id:                           <computed>\n" +
            "terraform:       instance_state:                    <computed>\n" +
            "terraform:       instance_type:                     \"t2.micro\"\n" +
            "terraform:       ipv6_address_count:                <computed>\n" +
            "terraform:       ipv6_addresses.#:                  <computed>\n" +
            "terraform:       key_name:                          <computed>\n" +
            "terraform:       network_interface.#:               <computed>\n" +
            "terraform:       network_interface_id:              <computed>\n" +
            "terraform:       password_data:                     <computed>\n" +
            "terraform:       placement_group:                   <computed>\n" +
            "terraform:       primary_network_interface_id:      <computed>\n" +
            "terraform:       private_dns:                       <computed>\n" +
            "terraform:       private_ip:                        <computed>\n" +
            "terraform:       public_dns:                        <computed>\n" +
            "terraform:       public_ip:                         <computed>\n" +
            "terraform:       root_block_device.#:               <computed>\n" +
            "terraform:       security_groups.#:                 <computed>\n" +
            "terraform:       source_dest_check:                 \"true\"\n" +
            "terraform:       subnet_id:                         \"subnet-f04d4d86\"\n" +
            "terraform:       tenancy:                           <computed>\n" +
            "terraform:       volume_tags.%:                     <computed>\n" +
            "terraform:       vpc_security_group_ids.#:          \"1\"\n" +
            "terraform:       vpc_security_group_ids.1365004845: \"sg-b92859c2\"\n" +
            "terraform: \n" +
            "terraform: \n" +
            "terraform: Plan: 1 to add, 0 to change, 0 to destroy.\n" +
            "terraform: \n" +
            "terraform: ------------------------------------------------------------------------\n" +
            "terraform: \n" +
            "terraform: This plan was saved to: /tmp/agent/prefork2636114080315546543/payload/_attachments/terraform/tf753032130994768466.plan\n" +
            "terraform: \n" +
            "terraform: To perform exactly these actions, run the following command to apply:\n" +
            "terraform:     terraform apply \"/tmp/agent/prefork2636114080315546543/payload/_attachments/terraform/tf753032130994768466.plan\"\n" +
            "terraform: ",
        "approved": false
    }
};
