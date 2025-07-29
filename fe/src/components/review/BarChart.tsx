interface props {
  percentage: number;
  num: number;
  score: number;
}
export default function BarChart({ percentage, num, score }: props) {
  return (
    <>
      <div className="flex wf ac gap-1">
        <span className="bar-chart-score fw-500">{score}</span>
        <div className="bar-chart wf">
          <div className="bar-chart-bar" style={{ width: `${percentage}%` }} />
        </div>
        <div className="bar-chart-percentage">{num}</div>
      </div>
    </>
  );
}
