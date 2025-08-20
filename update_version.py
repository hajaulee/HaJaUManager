import json
import os

input_file = "output-metadata.json"
packages_file = "packages.json"


with open(input_file, "r") as f:
    data = json.load(f)

with open(packages_file, "r") as f:
    packages = json.load(f)
    
for package in packages:
    package_name = package["packageName"]
    if package_name == data["applicationId"]:
        package["packageVersion"] = data["elements"][0]["versionName"]
        url_parts = package["packageUrl"].split("/")
        url_parts[-1] = data["elements"][0]["outputFile"]
        package["packageUrl"] = "/".join(url_parts)
        
with open(packages_file, "w") as f:
    json.dump(packages, f, indent=4)