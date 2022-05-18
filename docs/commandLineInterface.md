# Command Line Programs

draft 5/17/2022 for proto_version = 2.0.0

## Generate electionConfig

## Run Batch Encryption

````
Usage: RunBatchEncryption.kexe options_list
Options: 
    --inputDir, -in -> Directory containing input ElectionInitialized.protobuf file (always required) { String }
    --ballotDir, -ballots -> Directory to read Plaintext ballots from (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --invalidDir, -invalid -> Directory to write invalid input ballots to { String }
    --fixedNonces, -fixed -> Encrypt with fixed nonces and timestamp 
    --nthreads, -nthreads -> Number of parallel threads to use { Int }
    --help, -h -> Usage info 
````

Timing on Intel(R) Xeon(R) CPU E5-1650 v3 @ 3.50GHz,
Linux jlc 5.13.0-40-generic #45~20.04.1-Ubuntu SMP Mon Apr 4 09:38:31 UTC 2022 x86_64 x86_64 x86_64 GNU/Linux

single threaded:

````
Encryption with nthreads = 1 took 63702 millisecs for 100 ballots = 637 msecs/ballot
    12500 total encryptions = 125 per ballot = 5.09616 millisecs/encryption
````

With 12 threads (Intellij/jvm):

````
Encryption with nthreads = 12 took 11789 millisecs for 100 ballots = 118 msecs/ballot
    12500 total encryptions = 125 per ballot = 0.94312 millisecs/encryption
````

With 12 threads (IntelliJ/native):

````
Encryption with nthreads = 12 took 20949 millisecs for 100 ballots = 209 msecs/ballot
    12500 total encryptions = 125 per ballot = 1.67592 millisecs/encryption
````

Native executable:

````
$ build/bin/native/RunBatchEncryptionReleaseExecutable/RunBatchEncryption.kexe   -in src/commonTest/data/runWorkflowAllAvailable   -ballots src/commonTest/data/runWorkflowAllAvailable/private_data/input   -out testOut/RunBatchEncryption   -nthreads 12

Encryption with nthreads = 12 took 14155 millisecs for 100 ballots = 142 msecs/ballot
    12500 total encryptions = 125 per ballot = 1.1324 millisecs/encryption
````
