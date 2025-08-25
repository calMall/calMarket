import { FaRegCheckCircle } from "react-icons/fa";

interface props {
  status: OrderStatusUI;
}
export default function OrderStatus({ status }: props) {
  const statusOrder = { PENDING: 0, SHIPPED: 1, DELIVERED: 2 };
  const statusText = {
    DELIVERED: "配達完了",
    SHIPPED: "発送済み",
    PENDING: "処理中",
  };
  return (
    <div className="status-wrapper">
      {(["PENDING", "SHIPPED", "DELIVERED"] as OrderStatusUI[]).map(
        (s, idx, arr) => (
          <div key={s} className="status-step ">
            <FaRegCheckCircle
              className={`status-icon ${
                statusOrder[status] >= idx ? "active" : ""
              }`}
            />
            <span>{statusText[s]}</span>

            {/* 마지막 아이콘 전까지만 선 추가 */}
            {idx < arr.length - 1 && (
              <div
                className={`status-segment ${
                  statusOrder[status] > idx ? "active" : ""
                }`}
              />
            )}
          </div>
        )
      )}
    </div>
  );
}
