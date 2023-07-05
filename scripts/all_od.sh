urlshaCsv=$1

bash od.sh $urlshaCsv CustomClassIVMO InstanceTemplate
bash od.sh $urlshaCsv FileObjectFMO FileWriterTemplate
bash od.sh $urlshaCsv CaffeineCDMO CacheTemplate
bash od.sh $urlshaCsv StaticSVMO StaticTemplate
bash od.sh $urlshaCsv MockitoMutationOperator MockitoTemplate
bash od.sh $urlshaCsv DatabaseMutationOperator DatabaseTemplate
bash od.sh $urlshaCsv newFileNullODMO FileTemplate