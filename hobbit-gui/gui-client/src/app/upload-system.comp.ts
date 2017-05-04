import { Component } from '@angular/core';

@Component({
  selector: 'sg-upload-system',
  template: `
<sg-app-page-header>
    <div class="col-md-10">
       <h1>Upload System</h1>
    </div>
</sg-app-page-header>

<div class="container">
  <p>Every system that should be benchmarked on the Hobbit platform needs to be uploaded to it. This uploading comprises several steps. Note that a user profile is necessary to have the right to upload something to the platform.</p>
  <ol>
    <li>Before uploading a system, an adapter for the system needs to be developed, so that the system fits into the platform and is able to interact with the benchmark with which it should be benchmarked.</li>
    <li>The second step is the creation of a project in the gitlab instance of the platform.</li>
    <li>After creating a git project, you need to push the Docker image to the git project.</li>
    <li>The final step is to put the meta data describing your system into a file called <i>system.ttl</i> and push it to the git project.</li>
  </ol>

  <p><a href="https://github.com/hobbit-project/platform/wiki/Upload-a-system">All steps are described in detail in our wiki.</a></p>
</div>
  `
})
export class UploadSystemComponent {
}
