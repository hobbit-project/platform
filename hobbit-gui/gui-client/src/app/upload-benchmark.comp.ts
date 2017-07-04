import { Component } from '@angular/core';

@Component({
  selector: 'sg-upload-benchmark',
  template: `
<sg-app-page-header>
    <div class="col-md-10">
       <h1>Upload Benchmark</h1>
    </div>
</sg-app-page-header>

<div class="container">
  <p>Every benchmark that should be run on the Hobbit platform needs to be uploaded to it. This uploading comprises several steps. Note that a user profile is necessary to have the right to upload something to the platform.</p>
  <ol>
    <li>Before uploading, the benchmark has to be implemented in a way that it fits into the platform and is able to interact with the paltform controller.</li>
    <li>The second step is the creation of at least one project in the gitlab instance of the platform.</li>
    <li>After creating the git project(s), you need to push the Docker image(s) to the git project(s).</li>
    <li>The final step is to put the meta data describing the benchmark into a file called <i>benchmark.ttl</i> and push it to one of the git projects.</li>
  </ol>

  <p><a href="https://github.com/hobbit-project/platform/wiki/Upload-a-benchmark">All steps are described in detail in our wiki.</a></p>
</div>
  `
})
export class UploadBenchmarkComponent {
}
