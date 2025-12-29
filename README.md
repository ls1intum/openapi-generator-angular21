# OpenAPI Generator for Angular 21

[![Build](https://github.com/ls1intum/openapi-generator-angular21/actions/workflows/build.yml/badge.svg)](https://github.com/ls1intum/openapi-generator-angular21/actions)
[![Maven Central](https://img.shields.io/maven-central/v/de.tum.cit.aet/openapi-generator-angular21)](https://search.maven.org/artifact/de.tum.cit.aet/openapi-generator-angular21)

A custom [OpenAPI Generator](https://openapi-generator.tech/) for generating modern **Angular 21** TypeScript client code with best practices.

## Features

- **Signal-based `httpResource`** for GET requests (reactive, auto-refetching)
- **Injectable services** with `inject()` function for mutations (POST, PUT, DELETE)
- **Standalone services** (`providedIn: 'root'`, no NgModules required)
- **Strict TypeScript** with `readonly` modifiers on response models
- **Clean file organization** (separate files for services vs resources)
- **Compatible** with openapi-generator ecosystem (CLI, Maven, Gradle)

## Generated Code Example

### Models
```typescript
export interface User {
    readonly id: number;
    readonly login: string;
    readonly email?: string;
    readonly firstName?: string;
    readonly lastName?: string;
}

export interface UserCreate {
    login: string;
    email: string;
    firstName?: string;
    lastName?: string;
}
```

### API Service (Mutations)
```typescript
@Injectable({ providedIn: 'root' })
export class UserApi {
    private readonly http = inject(HttpClient);

    createUser(body: UserCreate): Observable<User> {
        return this.http.post<User>(`/api/users`, body);
    }

    updateUser(userId: number, body: UserUpdate): Observable<User> {
        return this.http.put<User>(`/api/users/${userId}`, body);
    }

    deleteUser(userId: number): Observable<void> {
        return this.http.delete<void>(`/api/users/${userId}`);
    }
}
```

### Resources (GET with httpResource)
```typescript
export interface GetAllUsersParams {
    search?: string;
    page?: number;
    size?: number;
}

export function getAllUsersResource(
    params?: Signal<GetAllUsersParams>
): HttpResourceRef<User[] | undefined> {
    return httpResource<User[]>(() => {
        const p = params?.() ?? {};
        const searchParams = new URLSearchParams();
        if (p.search) searchParams.set('search', p.search);
        if (p.page !== undefined) searchParams.set('page', String(p.page));
        if (p.size !== undefined) searchParams.set('size', String(p.size));
        const query = searchParams.toString();
        return `/api/users${query ? `?${query}` : ''}`;
    });
}

export function getUserResource(
    userId: Signal<number> | number
): HttpResourceRef<User | undefined> {
    return httpResource<User>(() => {
        const userIdValue = typeof userId === 'function' ? userId() : userId;
        return `/api/users/${userIdValue}`;
    });
}
```

## Installation

### Gradle (Artemis, other AET projects)

Add to your `build.gradle.kts`:

```kotlin
plugins {
    id("org.openapi.generator") version "7.18.0"
}

dependencies {
    // Add as a dependency to the openapi generator
    openapiGenerator("de.tum.cit.aet:openapi-generator-angular21:1.0.0")
}

openApiGenerate {
    generatorName.set("angular21")
    inputSpec.set("$projectDir/src/main/resources/openapi.yaml")
    outputDir.set("$buildDir/generated/openapi")
    
    configOptions.set(mapOf(
        "useHttpResource" to "true",
        "useInjectFunction" to "true",
        "separateResources" to "true",
        "readonlyModels" to "true"
    ))
}
```

Add to your `build.gradle` (Groovy DSL):

```groovy
plugins {
    id 'org.openapi.generator' version '7.18.0'
}

dependencies {
    // Add as a dependency to the openapi generator
    openapiGenerator 'de.tum.cit.aet:openapi-generator-angular21:1.0.0'
}

openApiGenerate {
    generatorName = 'angular21'
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = "$buildDir/generated/openapi"
    configOptions = [
        useHttpResource : 'true',
        useInjectFunction: 'true',
        separateResources: 'true',
        readonlyModels   : 'true'
    ]
}
```

### Maven

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.10.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>angular21</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <configOptions>
                    <useHttpResource>true</useHttpResource>
                    <useInjectFunction>true</useInjectFunction>
                    <separateResources>true</separateResources>
                    <readonlyModels>true</readonlyModels>
                </configOptions>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>de.tum.cit.aet</groupId>
            <artifactId>openapi-generator-angular21</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

### CLI

```bash
# Download the generator JAR
wget https://github.com/ls1intum/openapi-generator-angular21/releases/download/v1.0.0/openapi-generator-angular21-1.0.0.jar

# Generate code
java -cp openapi-generator-angular21-1.0.0.jar:openapi-generator-cli-7.10.0.jar \
    org.openapitools.codegen.OpenAPIGenerator generate \
    -g angular21 \
    -i openapi.yaml \
    -o ./generated
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `useHttpResource` | `true` | Use `httpResource` for GET requests instead of `HttpClient` |
| `useInjectFunction` | `true` | Use `inject()` function instead of constructor injection |
| `separateResources` | `true` | Generate separate `*-resources.ts` files for GET operations |
| `readonlyModels` | `true` | Add `readonly` modifier to response model properties |

## Usage in Components

```typescript
import { Component, computed, signal, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { 
    UserApi, 
    getUserResource, 
    getAllUsersResource,
    User, 
    UserCreate 
} from './generated';

@Component({
    selector: 'app-user-list',
    standalone: true,
    template: `
        @if (users.isLoading()) {
            <div class="spinner">Loading...</div>
        }

        @if (users.hasValue()) {
            @for (user of users.value(); track user.id) {
                <div (click)="selectUser(user.id)">
                    {{ user.firstName }} {{ user.lastName }}
                </div>
            }
        }

        @if (selectedUser.hasValue()) {
            <div class="details">
                Selected: {{ selectedUser.value()?.email }}
            </div>
        }
    `
})
export class UserListComponent {
    private readonly userApi = inject(UserApi);

    // Reactive state
    protected readonly page = signal(0);
    protected readonly selectedUserId = signal<number | undefined>(undefined);

    // Resources - automatically refetch when signals change
    protected readonly users = getAllUsersResource(
        computed(() => ({ page: this.page(), size: 20 }))
    );

    protected readonly selectedUser = getUserResource(
        computed(() => this.selectedUserId() ?? -1)
    );

    selectUser(id: number): void {
        this.selectedUserId.set(id);
    }

    async createUser(data: UserCreate): Promise<void> {
        const created = await firstValueFrom(this.userApi.createUser(data));
        console.log('Created:', created);
        this.users.reload(); // Refresh the list
    }
}
```

## Building from Source

```bash
git clone https://github.com/ls1intum/openapi-generator-angular21.git
cd openapi-generator-angular21
./gradlew build
```

## Trying the Example Generator

```bash
./gradlew generateExample
```

This generates Angular client code into `build/generated/example` using `example/example-openapi.yaml`.

## Publishing

```bash
# To GitHub Packages
./gradlew publish

# To Maven Local (for testing)
./gradlew publishToMavenLocal
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) file.

## Related Projects

- [Artemis](https://github.com/ls1intum/Artemis) - Interactive Learning with Automated Feedback
- [OpenAPI Generator](https://openapi-generator.tech/) - The base generator framework
