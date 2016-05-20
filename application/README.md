![alt tag](https://github.com/fogbow/arrebol/blob/pre-create/assets/ARREBOL-22.png)

# Arrebol
  
  Arrebol is an Interface for execution and monitoring of JDF-formated jobs
  
  It consists of three parts: the Application, the cli, and the gui
  
  The application is a layer created above the fogbow-scheduler that facilitates the retrieval of information on jobs or tasks in a readable way, while also persisting job information between diferent executions of the scheduler, and the rescalonation of jobs in case of failure
  
  The cli is an bash script that makes curl calls to the application endpoints, as follows

##How to Use

  Run Arrebol POST -j [jdf file path] optionals: -f [friendly name] -s [relative path to files]
  
  Run Arrebol GET for information on all running jobs
  
  Run Arrebol GET [job id or friendly name] for information on a specific running job
  
  Run Arrebol STOP [job id or friendly name] to stop an specific job
