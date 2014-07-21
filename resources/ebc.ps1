# --------------------------------------------------------------------------
# ebc Tools in Powershell
# (c) since 2014 by Burkhardt Renz, THM
# --------------------------------------------------------------------------
 
function usage{
$msg=
@'
This is ebc Rev 4.1
ebc starts tools for the ebc eBooks Collection 
in the current directory.

usage:
ebc -c checks the pathnames in the collection
ebc -i make a new index for the collection
ebc -s syncs the index 
ebc -d creates html directory for the collection
'@
echo $msg
}

if ("-c", "-i", "-s", "-d" -contains $args){
	echo "Loading ebc..."
	java -Xms64m -Xmx1024m -jar ebc.jar ebc.main $args 
}	else {
	usage
}	
