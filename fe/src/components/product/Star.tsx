export default function Star({ score }: { score: number }) {
  const totalStars = 5;
  const fullStars = Math.floor(score);
  const remainder = (score - fullStars) * 100;

  return (
    <div className="star-container">
      {[...Array(totalStars)].map((_, i) => (
        <div key={i} className="star-wrapper">
          <div className="star-back flex ac jc">★</div>
          <div
            className="star-front"
            style={{
              width:
                i < fullStars
                  ? "100%"
                  : i === fullStars
                  ? `${remainder}%`
                  : "0%",
            }}
          >
            ★
          </div>
        </div>
      ))}
      <span className="score">({score.toFixed(2)})</span>
    </div>
  );
}
