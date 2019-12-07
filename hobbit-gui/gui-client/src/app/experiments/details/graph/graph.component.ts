import { Component, OnInit, Input, ViewChild, ElementRef } from '@angular/core';
import { graphviz } from 'd3-graphviz';

@Component({
  selector: 'app-experiment-graph',
  template: '<div style="width: 600px; height: 600px;" #el></div>',
})
export class GraphComponent implements OnInit {
  @ViewChild('el')
  el: ElementRef;

  @Input()
  graph: string;

  constructor() { }

  ngOnInit() {
    try {
      graphviz(this.el.nativeElement)
      .attributer(function(d) {
        if (d.tag === 'polygon') {
          d.attributes.fill = 'transparent';
        }
      })
      .renderDot(this.graph);
    } catch (e) {
      this.el.nativeElement.style.removeProperty('width');
      this.el.nativeElement.style.removeProperty('height');
      this.el.nativeElement.textContent = 'Error while rendering';
    }
  }
}
