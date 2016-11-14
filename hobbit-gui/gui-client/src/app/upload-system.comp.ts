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
  <p>The upload of systems is realised by using GitLab provided by the University of Leipzig.<p>
  <a href="https://git.informatik.uni-leipzig.de/groups/hobbit">Gitlab main page for HOBBIT</a>
</div>
  `
})
export class UploadSystemComponent {
}
