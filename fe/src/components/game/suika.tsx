"use client";

import { useEffect, useRef } from "react";
import {
  Bodies,
  Body,
  Engine,
  Events,
  Render,
  Runner,
  World,
  IEventCollision,
} from "matter-js";
import { FRUITS_BASE, FRUITS_HLW } from "./fruits";

type Fruit = {
  name: string;
  radius: number;
};

const MatterGame = () => {
  const canvasRef = useRef<HTMLDivElement>(null);
  const currentBodyRef = useRef<Body | null>(null);
  const currentFruitRef = useRef<Fruit | null>(null);
  useEffect(() => {
    // =======================
    // 🎃 テーマ、果物設定
    // =======================
    let THEME: "base" | "halloween" = "halloween";
    let FRUITS: Fruit[] = FRUITS_BASE;

    switch (THEME) {
      case "halloween":
        FRUITS = FRUITS_HLW;
        break;
      default:
        FRUITS = FRUITS_BASE;
    }

    // =======================
    // ⚙️ Matter.js エンジン、レンダー
    // =======================
    if (!canvasRef.current) return;
    const engine = Engine.create();
    const render = Render.create({
      engine,
      element: canvasRef.current!,
      options: {
        wireframes: false,
        background: "#F7F4C8",
        width: canvasRef.current.clientWidth,
        height: canvasRef.current.clientHeight,
      },
    });
    engine.gravity.y = 1; // 重力の方向
    engine.gravity.scale = 0.002; // 速度調整
    const world = engine.world;
    console.log(engine.gravity);
    // =======================
    // 🧱 壁/床
    // =======================

    if (!render.options.width || !render.options.height) return;
    const W = render.options.width;
    const H = render.options.height;
    const leftWall = Bodies.rectangle(W * 0.025, H / 2, W * 0.05, H, {
      isStatic: true,
      render: { fillStyle: "#E6B143" },
    });
    const rightWall = Bodies.rectangle(W * 0.975, H / 2, W * 0.05, H, {
      isStatic: true,
      render: { fillStyle: "#E6B143" },
    });
    const ground = Bodies.rectangle(W / 2, H * 1, W, H * 0.07, {
      isStatic: true,
      render: { fillStyle: "#E6B143" },
    });
    const topLine = Bodies.rectangle(W / 2, H * 0.15, W, 2, {
      isStatic: true,
      isSensor: true,
      render: { fillStyle: "#E6B143" },
    });
    (topLine as any).name = "topLine";

    World.add(world, [leftWall, rightWall, ground, topLine]);

    Render.run(render);
    const runner = Runner.create();
    Runner.run(runner, engine);

    // =======================
    // 🎮 ゲーム状態
    // =======================

    let disableAction = false;
    let interval: number | null = null;

    // =======================
    // 🍒 果物生成
    // =======================
    function addFruit() {
      const index = Math.floor(Math.random() * 5);
      const fruit = FRUITS[index];
      if (!canvasRef.current) return;
      const body = Bodies.circle(
        canvasRef.current.clientWidth / 2,
        20,
        fruit.radius,
        {
          isSleeping: true,
          restitution: 0.2,
          render: {
            sprite: {
              texture: `${fruit.name}.png`,
              xScale: 1,
              yScale: 1,
            },
          },
        }
      ) as Body & { index: number };

      body.index = index;

      currentBodyRef.current = body;
      currentFruitRef.current = fruit;

      World.add(world, body);
    }

    // =======================
    // ⌨️ キーボードイベント
    // =======================
    const handleKeyDown = (event: KeyboardEvent) => {
      const currentBody = currentBodyRef.current;
      const currentFruit = currentFruitRef.current;

      if (disableAction) return;

      switch (event.code) {
        case "KeyA":
          if (interval) return;

          interval = window.setInterval(() => {
            if (
              currentBody &&
              currentFruit &&
              currentBody.position.x - currentFruit.radius > 30
            ) {
              Body.setPosition(currentBody, {
                x: currentBody.position.x - 1,
                y: currentBody.position.y,
              });
            }
          }, 5);
          break;

        case "KeyD":
          if (interval) return;

          interval = window.setInterval(() => {
            if (
              currentBody &&
              currentFruit &&
              currentBody.position.x + currentFruit.radius < 590
            ) {
              Body.setPosition(currentBody, {
                x: currentBody.position.x + 1,
                y: currentBody.position.y,
              });
            }
          }, 5);
          break;

        case "KeyS":
          if (currentBody) {
            currentBody.isSleeping = false;
          }
          disableAction = true;

          setTimeout(() => {
            addFruit();
            disableAction = false;
          }, 1000);
          break;
      }
    };

    const handleKeyUp = (event: KeyboardEvent) => {
      switch (event.code) {
        case "KeyA":
        case "KeyD":
          if (interval) {
            clearInterval(interval);
            interval = null;
          }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    // =======================
    // 💥 衝突イベント
    // =======================
    const collisionHandler = (event: IEventCollision<Engine>) => {
      event.pairs.forEach((collision) => {
        const bodyA = collision.bodyA as Body & {
          index?: number;
          name?: string;
        };
        const bodyB = collision.bodyB as Body & {
          index?: number;
          name?: string;
        };

        if (
          bodyA.index !== undefined &&
          bodyB.index !== undefined &&
          bodyA.index === bodyB.index
        ) {
          const index = bodyA.index;

          if (index === FRUITS.length - 1) return;

          World.remove(world, [bodyA, bodyB]);

          const newFruit = FRUITS[index + 1];
          const newBody = Bodies.circle(
            collision.collision.supports[0].x,
            collision.collision.supports[0].y,
            newFruit.radius,
            {
              render: {
                sprite: {
                  texture: `${newFruit.name}.png`,
                  xScale: 1,
                  yScale: 1,
                },
              },
            }
          ) as Body & { index: number };

          newBody.index = index + 1;
          World.add(world, newBody);
        }

        if (
          !disableAction &&
          (bodyA.name === "topLine" || bodyB.name === "topLine")
        ) {
          disableAction = true; // 一度だけ実行されるように防止
          alert("ゲームオーバー");
          window.location.href = "/"; // ホームに移動
        }
      });
    };
    Events.on(engine, "collisionStart", collisionHandler);

    // =======================
    // 🚀 ゲーム開始
    // =======================
    addFruit();

    // =======================
    // cleanup
    // =======================
    return () => {
      Render.stop(render);
      World.clear(world, false);
      Engine.clear(engine);
      render.canvas.remove();
      render.textures = {};

      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      Events.off(engine, "collisionStart", collisionHandler);
    };
  }, []);

  return (
    <div className="flex jc hf wf">
      <div style={{ width: "30%", height: "80%" }} ref={canvasRef}></div>
    </div>
  );
};

export default MatterGame;
