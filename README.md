# Iterated-Prisoners-Dilemma

This code initialize a GUI and allow the agents to play witch each other.

Command lines needed to compile:
javac -classpath "Path/to/jade.jar;./" MainAgent.java GUI.java PlayerStats.java agents/*.java

Command lines needed to run:
java -classpath "Path/to/jade.jar;./" jade.Boot -agents " Main:MainAgent;SPITEFUL:agents.Spiteful_agent;TFT:agents.TFT_agent;Pavlov:agents.Pavlov_agent;TFT_SPIT_16:agents.TFTSPIT_16;Gradual_16:agents.Gradual_16;QLearning_16:agents.QLearning_16;CCSPITE_16:agents.CCSPITE_16;AdaptivePavlov_16:agents.AdaptivePavlov_16"
