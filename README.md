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
export interface Course {
    readonly id: number;
    readonly title: string;
    readonly shortName: string;
    readonly description?: string;
    readonly startDate?: string;
    readonly endDate?: string;
    readonly semester?: string;
    readonly testCourse?: boolean;
    readonly onlineCourse?: boolean;
    readonly maxComplaints?: number;
    readonly maxTeamComplaints?: number;
    readonly maxComplaintTimeDays?: number;
    readonly studentGroupName?: string;
    readonly teachingAssistantGroupName?: string;
    readonly editorGroupName?: string;
    readonly instructorGroupName?: string;
    readonly color?: string;
    readonly courseIcon?: string;
}

export interface CourseCreate {
    title: string;
    shortName: string;
    description?: string;
    startDate?: string;
    endDate?: string;
    semester?: string;
    testCourse?: boolean;
    onlineCourse?: boolean;
    color?: string;
}

export interface CourseUpdate {
    title?: string;
    description?: string;
    startDate?: string;
    endDate?: string;
    semester?: string;
    color?: string;
}
```

### API Service (Mutations)
```typescript
@Injectable({ providedIn: 'root' })
export class CourseApi {
    private readonly http = inject(HttpClient);
    private readonly basePath = '/api';

    createCourse(courseCreate: CourseCreate): Observable<Course> {
        const url = `${this.basePath}/courses`;
        return this.http.post<Course>(url, courseCreate);
    }

    deleteCourse(courseId: number): Observable<void> {
        const url = `${this.basePath}/courses/$${courseId}`;
        return this.http.delete(url);
    }

    updateCourse(courseId: number, courseUpdate: CourseUpdate): Observable<Course> {
        const url = `${this.basePath}/courses/$${courseId}`;
        return this.http.put<Course>(url, courseUpdate);
    }
}
```

### Resources (GET with httpResource)
```typescript
const BASE_PATH = '/api';

export interface GetAllCoursesParams {
    onlyActive?: boolean;
    page?: number;
    size?: number;
}

export function getAllCoursesResource(params?: Signal<GetAllCoursesParams>): HttpResourceRef<Array<Course> | undefined> {
    return httpResource<Array<Course>>(() => {
        const queryParams = params?.() ?? {};
        const searchParams = new URLSearchParams();
        if (queryParams.onlyActive !== undefined) {
            searchParams.set('onlyActive', String(queryParams.onlyActive));
        }
        if (queryParams.page !== undefined && queryParams.page !== null) {
            searchParams.set('page', String(queryParams.page));
        }
        if (queryParams.size !== undefined && queryParams.size !== null) {
            searchParams.set('size', String(queryParams.size));
        }
        const query = searchParams.toString();
        return `${BASE_PATH}/courses${query ? `?${query}` : ''}`;
    });
}

export function getCourseResource(courseId: Signal<number> | number): HttpResourceRef<Course | undefined> {
    return httpResource<Course>(() => {
        const courseIdValue = typeof courseId === 'function' ? courseId() : courseId;
        return `${BASE_PATH}/courses/${courseIdValue}`;
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
    <version>7.18.0</version>
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
java -cp openapi-generator-angular21-1.0.0.jar:openapi-generator-cli-7.18.0.jar \
    org.openapitools.codegen.OpenAPIGenerator generate \
    -g angular21 \
    -i openapi.yaml \
    -o ./generated
```

## Configuration Options

| Option              | Default | Description                                                 |
|---------------------|---------|-------------------------------------------------------------|
| `useHttpResource`   | `true`  | Use `httpResource` for GET requests instead of `HttpClient` |
| `useInjectFunction` | `true`  | Use `inject()` function instead of constructor injection    |
| `separateResources` | `true`  | Generate separate `*-resources.ts` files for GET operations |
| `readonlyModels`    | `true`  | Add `readonly` modifier to response model properties        |

## Usage in Components

```typescript
import { Component, computed, signal, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { 
    CourseApi,
    getCourseResource, 
    getAllCoursesResource,
    Course, 
    CourseCreate,
    CourseUpdate 
} from './generated';

@Component({
    selector: 'app-course-list',
    standalone: true,
    template: `
        @if (courses.isLoading()) {
            <div class="spinner">Loading...</div>
        }

        @if (courses.hasValue()) {
            @for (course of courses.value() ?? []; track course.id) {
                <div (click)="selectCourse(course.id)">
                    {{ course.title }}
                </div>
            }
        }

        @if (selectedCourse.hasValue()) {
            <div class="details">
                Selected: {{ selectedCourse.value()?.shortName }}
            </div>
        }
    `
})
export class CourseListComponent {
    private readonly courseApi = inject(CourseApi);

    // Reactive state
    protected readonly page = signal(0);
    protected readonly selectedCourseId = signal<number | undefined>(undefined);

    // Resources - automatically refetch when signals change
    protected readonly courses = getAllCoursesResource(
        computed(() => ({ page: this.page(), size: 20, onlyActive: true }))
    );

    protected readonly selectedCourse = getCourseResource(
        computed(() => this.selectedCourseId() ?? -1)
    );

    selectCourse(id: number): void {
        this.selectedCourseId.set(id);
    }

    async createCourse(data: CourseCreate): Promise<void> {
        const created = await firstValueFrom(this.courseApi.createCourse(data));
        console.log('Created:', created);
        this.courses.reload(); // Refresh the list
    }

    async updateCourse(courseId: number, data: CourseUpdate): Promise<void> {
        const updated = await firstValueFrom(this.courseApi.updateCourse(courseId, data));
        console.log('Updated:', updated);
        this.courses.reload(); // Refresh the list
    }

    async deleteCourse(courseId: number): Promise<void> {
        await firstValueFrom(this.courseApi.deleteCourse(courseId));
        this.courses.reload(); // Refresh the list
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
