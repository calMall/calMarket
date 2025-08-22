export default function BarChartLoading({ score }: { score: number }) {
  return (
    <div className="flex wf ac gap-1">
      <span className="bar-chart-score fw-500">{score}</span>
      <div className="bar-chart wf loading-box"></div>
    </div>
  );
}
