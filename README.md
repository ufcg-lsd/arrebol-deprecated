![alt tag](https://github.com/fogbow/arrebol/blob/pre-create/assets/ARREBOL-22.png)

# Arrebol
  
  Arrebol is an Interface for execution and monitoring of JDF-formated jobs
  
  It consists of three parts: the Application, the cli, and the gui
  
  The application is a layer created above the fogbow-scheduler that facilitates the retrieval of information on jobs or tasks in a readable way, while also persisting job information between diferent executions of the scheduler, and the rescalonation of jobs in case of failure
  
##How to Use

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

  To create a Jdf job run: 
  ```
  bash arrebol.sh POST -j [jdf file path] optionals: -f [friendly name] -s [relative path to files] 
  ```
  
  For information on all running jobs, run:
  ```
  bash arrebol.sh GET 
  ```
  For information on a specific running job, run:
  ```
  bash arrebol.sh GET [job id or friendly name]
  ```
  To stop an specific job, run:
  ```
  bash arrebol.sh STOP [job id or friendly name] 
  ```
  
  The gui is a browser application for visualy monitoring the state of the jdf jobs
  