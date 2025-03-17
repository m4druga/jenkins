pipeline {
    agent { label 'docker-host' }
    options {
        disableConcurrentBuilds()
        disableResume()
    }
    parameters {
        choice(name: 'DATABASE_ENGINE', choices: ['mysql', 'postgres'], description: 'Select the database engine')
        string(name: 'ENVIRONMENT_NAME', trim: true, description: 'Environment name (used for Docker image and container)')
        password(name: 'DB_PASSWORD', defaultValue: '', description: 'Password for the DB root user (MySQL) or POSTGRES_PASSWORD (Postgres)')
        string(name: 'DB_PORT', trim: true, description: 'Port to expose the database service')
        booleanParam(name: 'SKIP_STEP_1', defaultValue: false, description: 'Skip Docker image build step')
    }
    stages {
        stage('Validate Parameters') {
            steps {
                script {
                    def port
                    try {
                        port = params.DB_PORT.toInteger()
                    } catch (Exception e) {
                        error("DB_PORT must be a valid number between 1 and 65535")
                    }
                    if (port <= 0 || port >= 65536) {
                        error("DB_PORT must be a valid number between 1 and 65535")
                    }
                }
            }
        }
        stage('Checkout Repository') {
            steps {
                script {
                    git branch: 'master',
                        credentialsId: '97489484-1169-4cba-ae7e-761162392580',
                        url: 'git@github.com:m4druga/jenkins.git'
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    if (!params.SKIP_STEP_1) {
                        if (params.DATABASE_ENGINE == 'mysql') {
                            echo "Building MySQL Docker image ${params.ENVIRONMENT_NAME}:latest using port ${params.DB_PORT}"
                            sh """
                                sed 's/<PASSWORD>/${params.DB_PASSWORD}/g' pipelines/include/create_developer_mysql.template > pipelines/include/create_developer_mysql.sql
                            """
                            sh "docker build -f pipelines/Dockerfile.mysql -t ${params.ENVIRONMENT_NAME}:latest pipelines/"
                        } else if (params.DATABASE_ENGINE == 'postgres') {
                            echo "Building PostgreSQL Docker image ${params.ENVIRONMENT_NAME}:latest using port ${params.DB_PORT}"
                            sh """
                                sed 's/<PASSWORD>/${params.DB_PASSWORD}/g' pipelines/include/create_developer_psql.template > pipelines/include/create_developer_psql.sql
                            """
                            sh "docker build -f pipelines/Dockerfile.psql -t ${params.ENVIRONMENT_NAME}:latest pipelines/"
                        }
                    } else {
                        echo "Skipping Docker image build as SKIP_STEP_1 is enabled."
                    }
                }
            }
        }
        stage('Run Container and Configure Database') {
            steps {
                script {
                    def dateTime = sh(script: "date +%Y%m%d%H%M%S", returnStdout: true).trim()
                    def containerName = "${params.ENVIRONMENT_NAME}_${dateTime}"

                    if (params.DATABASE_ENGINE == 'mysql') {
                        sh """
                            docker run -itd --name ${containerName} --rm -e MYSQL_ROOT_PASSWORD=${params.DB_PASSWORD} -p ${params.DB_PORT}:3306 ${params.ENVIRONMENT_NAME}:latest
                        """
                        def maxRetries = 10
                        def attempt = 0
                        def isReady = false
                        while (attempt < maxRetries && !isReady) {
                            echo "Checking MySQL readiness, attempt ${attempt + 1}"
                            try {
                                def output = sh(script: "docker exec ${containerName} mysqladmin ping --silent --user=root --password=${params.DB_PASSWORD}", returnStdout: true).trim()
                                if (output.contains("mysqld is alive")) {
                                    isReady = true
                                    echo "MySQL is ready."
                                } else {
                                    sleep 5
                                }
                            } catch(Exception e) {
                                echo "MySQL not ready, retrying..."
                                sleep 5
                            }
                            attempt++
                        }
                        if (!isReady) {
                            error("MySQL did not become ready in time.")
                        }
                        sh """
                            docker exec ${containerName} /bin/bash -c 'mysql --user="root" --password="${params.DB_PASSWORD}" < /scripts/create_developer_mysql.sql'
                        """
                    } else if (params.DATABASE_ENGINE == 'postgres') {
                        sh """
                            docker run -itd --name ${containerName} --rm -e POSTGRES_PASSWORD=${params.DB_PASSWORD} -e POSTGRES_DB=devapp -p ${params.DB_PORT}:5432 ${params.ENVIRONMENT_NAME}:latest
                        """
                    }
                    echo "Docker container created: ${containerName}"
                }
            }
        }
    }
}