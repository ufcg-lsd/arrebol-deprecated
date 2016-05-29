![alt tag](https://github.com/fogbow/arrebol/blob/pre-create/assets/ARREBOL-22.png)

# Arrebol
## What is Arrebol?
  Arrebol is a tool for monitoring and executing jobs in a multi-cloud environment federated by the [fogbow middleware](http://www.fogbowcloud.org). Arrebol allows the user to harness cloud resources without bothering about the details fo the cloud infrastructure.

  Arrebol has three main components:
  - **Submission service**: The submission service is the deamon responsible for receiving job submission and monitoring requests and interacting with the **fogbow middleware** to execute the jobs in the federated cloud resources. The submission services runs a **REST** interface acessed by two clients: **Arrebol CLI** and **Arrebol Dashboard**.
  - **Arrebol CLI**: CLI is a bash script to easy interaction with the **submission service** in UNIX enviroments. It allows to submit jobs, retrieve status information about running jobs, and cancel them.
  - **Arrebol Dashboard**: Dashboard is a web application that shows status information about the jobs controlled by a **submission service**.

  This document provides a short guide to use the **Arrebol CLI** to interact with the **Submission Service**. It also describes how to install and configure the **Submission Service** and the **Arrebol Dashboard**.

##How to use it?

### Writing a job

A job description file, or jdf for short, is a plain text file that contains a job description. Each job description file describes a single job. You can edit a jdf in the text editor of your choice. By convention, we use the .jdf extension to name all job description files.

A jdf file has two types of clauses in it: the job and the task clauses. You use the first type to specify common attributes for the entire job and the other one to specify particular attributes and commands to the tasks that comprise your parallel application.

Clause | Description
---- | --------------------
job: |
label: | A desciptive name for the job.
task: |
init: | Input data. The files that will be staged into the Worker.
remote: | Code for execution, which is a command line on the target resource.
final: | Output data. The files that will be staged out from the Worker.

#### The Job clause

A job clause contains a (possibly empty) list of sub-clauses. For instance, the requirements sub-clause encompasses the list of requirements that need to be fulfilled by a worker node, so that it can be selected to run tasks of the job, while the label sub-clause associates a name to the job. This sub-clause is useful for tracking the execution of the job and also to associate the name of the output files to the job that has generated it.

Below we present an example of a jdf, which defines a very simple job named myjob1. It requires worker nodes to run in the cloud of the federation member named **memberOne**. Also, the worker nodes must have the mem attribute set to a number greater or equal to 1024 and .

    job:
    label:myjob1
    requirements : Glue2RAM >= 1024 AND Glue2CloudComputeManagerID==memberOne
    task:
    remote: mytask

As we mentioned before, all sub-clauses of a job clause are optional. If the label sub-clause does not exist in the jdf, an internal job id is used to identify it. If there is no requirements sub-clause, the Broker assumes that all worker nodes in your grid are able to run the tasks of your job.

Besides label and requirements sub-clauses, you may define default descriptions for all tasks of a job. This is further explained below.

### Arrebol CLI

#### Running a job
After unpacking a **Arrebol** release package (find the [here](https://github.com/fogbow/arrebol/releases)), the **Arrebol CLI** script can be found in ```bin/```directory.

The **Arrebol** jobs are described in the JDF format. In this format, the user can

To create a Jdf-formatted job:
```
bash bin/arrebol.sh POST jdf_file_path optionals: -f [friendly name] -s [relative path to files]
```
To retrieve status information about all running jobs:
```
bash bin/arrebol.sh GET
```
To retrieve information about a specific running job:
```
bash bin/arrebol.sh GET [job id or friendly name]
```
To stop a specific job:
```
bash arrebol.sh STOP [job id or friendly name]
```

TODO: explain JDF syntax

TODO: explain how to configure CLI

TODO: explain how to configure submission service

TODO: explain how to configure dashboard

  To start the application layer run:

```
java ArrebolMain [path to configuration file]
```
  Following is the example of a configuration file:

```
infra_is_elastic=true
infra_provider_class_name=org.fogbowcloud.scheduler.infrastructure.fogbow.FogbowInfrastructureProvider
infra_order_service_time=100000
infra_resource_service_time=100000
infra_resource_connection_timeout=300000
infra_resource_idle_lifetime=30000

infra_initial_specs_file_path=/home/username/Dev/sebalScheduleEnv/initialSpec
infra_initial_specs_block_creating=true

infra_fogbow_manager_base_url=http://188.188.15.81:8182
infra_fogbow_token_public_key_filepath=/home/username/Dev/keys/cert

accounting_datastore_url=jdbc:h2:/home/username/Dev/sebalScheduleEnv/h2db/orders
execution_monitor_period=60000
local.output = /home/username/Dev/sebalScheduleEnv/result
local_command_interpreter=/bin/bash

fogbow.voms.certificate.password=password
fogbow.voms.server=vomsserver

rest_server_port=44444
```

this is the Initial Spec file:

```
[
{"image":"fogbow-ubuntu",
 "username":"fogbow",
 "publicKey":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAACAQDG2U8rz4I31LIyDBPpe01WJdGt0JBowZ0Zq7Nxq7mol3G4cW5OJt9v3aQLRU8zanceXXSagNg8O4v2ppFzROYlIOgg2KN3Zu6Tj7Evmfp++J160dwshnP3aQCSLIDSBnMsZyPRprIbaL2LifVmrKcOfG3QcRQHZx2HRWJp+lty0IqP+FBaobB7nXzF58ibOJ84Fk9QpQmS5JK3AXdwCISmN8bgfcjoUJB2FMB5OU8ilkIyG4HDZmI82z+6hUS2sVd/ss8biIN6qGfRVxEDhVlDw3o+XqL+HQ7udd2Q61oHs8iBa711SWG64Eie6HAm8SIOsL7dvPx1rBfBsp3DqbcvjsnfNKsgkj5wnf7E8q9S6rTiDQndCGWvAnSU01BePD51ZnMEckluYTOhNLgCMtNTXZJgYSHPVsLWXa5xdGSffL73a4gIupE36tnZlNyiAQGDJUrWh+ygEc2ALdQfpOVWo+CMkTBswvrHYSJdFC7r1U8ACrOlsLE02/uqqBbp7fTUuuMk77J8t0ocxuz48tVKOlog0ajS5nphPLfPGnP2PVTh7GXNTLOnqGVwMrjFIAHj7ukd+l36wUAIHR7Y4YWKVaIBvTZS/fQNn0cOGon2DnNL3wNAUc6pthhXlNY33aU2ky55mZR4drAdbRGRdEZQF0YHEFnzP0x2GucHwg6ZtMJ2Aw== username@machine",
 "privateKeyFilePath":"/home/username/.ssh/id_rsa",
 "requirements":{
	"FogbowRequirements":"Glue2RAM >= 1024 ",
	"RequestType":"one-time"}
	}
]
```

  The cli is an bash script that makes curl calls to the application endpoints, as follows

##How to Use

  ```

  The gui is a browser application for visualy monitoring the state of the jdf jobs