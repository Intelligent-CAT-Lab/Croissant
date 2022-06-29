projects=(
    "https://github.com/apache/commons-csv.git"
    "https://github.com/apache/commons-cli.git"
    "https://github.com/FasterXML/jackson-core.git"
    "https://github.com/jhy/jsoup.git"
    "https://github.com/google/gson.git"
    "https://github.com/apache/commons-codec.git"
    "https://github.com/apache/commons-compress.git"
)
for project in ${projects[@]}; do
    git clone ${project}
done

