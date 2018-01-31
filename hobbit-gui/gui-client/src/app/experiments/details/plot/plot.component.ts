import { Diagram } from './../../../model';
import { Component, OnInit, Input, ViewChild, ElementRef } from '@angular/core';
import Chart from 'chart.js';
import randomColor from 'randomcolor';

@Component({
  selector: 'app-experiment-plot',
  template: `
    <div style="overflow-x: scroll;">
      <canvas #canvas width="600" height="400"></canvas>'
    </div>
    `
})
export class PlotComponent implements OnInit {

  @ViewChild('canvas')
  canvas: ElementRef;

  @Input()
  diagram: Diagram;

  constructor() { }

  ngOnInit() {
    const chartCanvas = <HTMLCanvasElement>this.canvas.nativeElement;
    const ctx = chartCanvas.getContext('2d');
    const color = randomColor();
    const data: any = {
      labels: this.diagram.data.map(d => d.x),
      datasets: [{
        label: this.diagram.label + '',
        data: this.diagram.data.map(d => d.y),
        borderColor: color,
        backgroundColor: color,
      }],
    };
    const myChart = new Chart(ctx, {
      type: 'line',
      data,
      options: {
        responsive: false,
      }
    });
  }

}
