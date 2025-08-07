import Image from "next/image";

const boxStyle = {
  border: "1px solid #ccc",
  borderRadius: "6px",
  padding: "1rem",
  backgroundColor: "#f9f9f9",
  lineHeight: "1.6",
  marginBottom: "1.5rem",
};

export default function OrderDetail() {
  const data: OrderDetailResponseDto = {
    message: "success",
    order: {
      itemName:
        "【公式_商品：送金無料】デスクトップパソコン一体型office付き商品おすすめ　富士通FMV DESKTOP F WF-1 K1【FH75-k1ベースモデル】23.8型Windows11 Home Celeron メモリ4GB SSD 256GB Office 搭載モデル RK_WF1K1_A002",
      orderId: 12345,
      itemCode: "ITEM789",
      price: 5800,
      quantity: 2,
      date: "2025-07-28",
      imageList: [
        "https://thumbnail.image.rakuten.co.jp/@0_mall/best1mobile/cabinet/compass1704878231.jpg?_ex=128x128",
      ],
      deliveryAddress: "東京都千代田区神田練塀町300番地",
      orderDate: "2025-07-27T10:30:00Z",
    },
  };

  const total = data.order.price * data.order.quantity;

  return (
    <div
      className="order-container pd-1"
      style={{
        maxWidth: "600px",
        margin: "auto",
        background: "#fff",
      }}
    >
      <h2>注文の詳細</h2>

     
      <div style={{ display: "flex", alignItems: "center", marginBottom: "1rem" }}>
        <Image
          alt="product"
          src={data.order.imageList[0]}
          width={128}
          height={128}
        />

       
        <div
          className="product-info"
          style={{
            fontWeight: "bold",
            marginLeft: "1rem",
            fontSize: "0.9rem",
            lineHeight: "1.5",
          }}
        >
          <p>【公式_商品：送金無料】デスクトップパソコン一体型office付き商品おすすめ 富士通
          FMV DESKTOP F WF-1 K1 【FH75-k1ベースモデル】 23.8型Windows11 Home Celeron
          メモリ4GB SSD 256GB Office 搭載モデル RK_WF1K1_A002</p>
        </div>
      </div>

      <div className="order-date" style={{ marginBottom: "1rem" }}>
        注文日：{new Date(data.order.orderDate).toLocaleDateString("ja-JP")}
      </div>

      <h3
        style={{
          fontWeight: "bold",
          marginTop: "1rem",
          marginBottom: "0.5rem",
        }}
      >
        お届け先
      </h3>
      <div className="box address-box" style={boxStyle}>
        何とか
        <br />
        江戸川区
        <br />
        1-2-3
        <br />
        ガナダラマパサ101
        <br />
        東京都　123-1234
        <br />
        日本
      </div>

      <h3 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>領収書</h3>
      <div className="box receipt-box" style={boxStyle}>
        <p>商品の数量：{data.order.quantity}</p>
        <p>商品の小計：￥{data.order.price * data.order.quantity}</p>
        <p>配送料・手数料：￥0</p>
        <p>注文合計：￥{total}</p>
      </div>
    </div>
  );
}
