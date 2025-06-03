#!/bin/bash

if [[ $# -lt 3 ]]; then
    echo "Usage: $0 <BASE_URL> <INSTANCE_ID> <TOKEN>"
    exit 1
fi

BASE_URL=$1
INSTANCE_ID=$2
TOKEN=$3

OUTPUT_DIR="./code-coverage"

FILES=("coverage.info" "flows.zip")
UNZIP_DIR="${OUTPUT_DIR}"

mkdir -p "$OUTPUT_DIR" "$UNZIP_DIR"

download_file() {
    local file_name=$1
    local download_url="${BASE_URL}/api/v1/process/${INSTANCE_ID}/attachment/${file_name}"
    local output_file="${OUTPUT_DIR}/${file_name}"

    echo "Downloading ${file_name} from ${download_url}..."

    if curl -H "Authorization: ${TOKEN}" -o "${output_file}" -L "${download_url}"; then
        echo "File downloaded successfully: ${output_file}"
    else
        echo "Failed to download the file: ${download_url}"
        exit 1
    fi
}

for file in "${FILES[@]}"; do
    download_file "$file"
done

ZIP_FILE="${OUTPUT_DIR}/flows.zip"

echo "Unzipping ${ZIP_FILE} into ${UNZIP_DIR}..."

if unzip -o "$ZIP_FILE" -d "$UNZIP_DIR"; then
    echo "File unzipped successfully to: ${UNZIP_DIR}"
else
    echo "Failed to unzip file: ${ZIP_FILE}"
    exit 1
fi

if (cd "${OUTPUT_DIR}" && genhtml "coverage.info" --output-directory "html"); then
    echo "HTML report generated successfully in: ${OUTPUT_DIR}/html"
else
    echo "Failed to generate HTML report"
    exit 1
fi
