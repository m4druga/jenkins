# Jenkins Pipeline for Database Environments

## Overview

This Jenkins pipeline automates the setup and management of MySQL and PostgreSQL database environments using Docker. 
It supports multi-database deployment, user creation, and database initialization with predefined structures.

This archive contains all the files used in the current version of the pipeline:

```
|-- Dockerfile.mysql
|-- Dockerfile.psql
|-- build-dev-environment.groovy
`-- include
    |-- create_developer_mysql.template
    `-- create_developer_psql.template
```

## Features

- **Supports MySQL and PostgreSQL**: Users can select the database engine at runtime.
- **Automated Database Setup**: The pipeline initializes databases, creates necessary tables, and configures user permissions.
- **Parameterized Builds**: Allows flexible configuration via Jenkins parameters.
- **Improved Error Handling**: Validates input parameters and checks service readiness before proceeding.
- **Scalable and Modular**: The approach ensures easy extension and maintenance.

## Approach and Enhancements

This project refactored and improved the original pipeline to address issues and introduce new functionalities. Below are the key enhancements:

### 1. Bug Fixes and Process Improvements

- **Fixed incorrect user creation in MySQL**: Ensured database existence before creating users.
- **Validated database port input**: Prevents misconfigurations due to invalid port numbers.
- **Added MySQL readiness check**: Avoids executing SQL scripts before the service is available.
- **Enhanced user privileges**: MySQL users now support external connections.

### 2. Feature Enhancements

- **Multi-Database Support**: Introduced PostgreSQL as an alternative to MySQL.
- **Database Schema Initialization**: Added `departments` table with sample data.
- **Improved Parameterization**: Users can now specify database engine, port, and credentials.

### 3. Pipeline Optimization

- **Refactored Docker Build Process**: Separate Dockerfiles for MySQL and PostgreSQL.
- **Optimized Container Initialization**: Ensured proper sequencing of database setup steps.
- **Improved Jenkins Stages**: Added validation and conditional execution logic.

## Usage

1. Configure the pipeline in Jenkins.
2. Provide the required parameters:
   - `DATABASE_ENGINE`: Choose between `mysql` or `postgres`.
   - `ENVIRONMENT_NAME`: Name of the database environment.
   - `DB_PASSWORD`: Root password for MySQL or `POSTGRES_PASSWORD` for PostgreSQL.
   - `DB_PORT`: Port to expose the database service.
   - `SKIP_STEP_1`: Skip Docker image build step if already built.
3. Trigger the pipeline and monitor execution logs.