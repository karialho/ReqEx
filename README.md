# ReqEx
ReqEx is a standalone export tool for Micro Focus Caliber. It provides exporting projects/requirement types from Caliber into Excel (.xlsx) or ReqIF files. Caliber is a nice requirements management tool. Easy to use and adopt, but sufficient for demanding use in terms of features. However, the technology base is outdated and the vendor will end technical support at the end of year 2021.

[Requirements Interchange Format](https://www.omg.org/reqif/) (ReqIF) is OMG's standard for transferring requirements and specifications from one system and organisation to another. That is why it is an ideal file format for migrating Caliber data into modern requirements tools with ReqIF import capabilities.  

Building ReqEx requires the proprietary Caliber SDK Java library (version 11.5 or higher) that is distributed with Caliber. The SDK takes care of connecting to a Caliber server, and you naturally also need to have valid user credentials with rights to the projects you wish to export.

The ReqEx tool is developed and copyrighted by ImproveIt Oy, and licensed with the Apache License 2.0.

For information on building and using the tool, please refer tpo documentation in the [docs](docs) directory.
