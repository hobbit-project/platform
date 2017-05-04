# Analysis Component for the HOBBIT Platform

This project implements the Analysis Component of the HOBBIT Platform.

The Analysis Component reads the evaluation results from the storage, processes them and stores additional information in the storage.

The purpose of the Analysis Component is to enhance
the benchmark results by combining them with the features of the benchmarked system and the data
or task generators. These combination can lead to additional insights, e.g., strengths and weaknesses
of a certain system.

## Notes

This is a very early version of the Analysis Component.

Current assumptions:

* The controller2AnalysisQueue is initialized by another component.
* An exception is thrown if the Graph URI is null. 

## Next steps:

* Enhance Analysis Component with more functionalities.
* Refactor.
